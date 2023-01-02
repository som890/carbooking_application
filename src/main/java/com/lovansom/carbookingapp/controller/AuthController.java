package com.lovansom.carbookingapp.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lovansom.carbookingapp.model.ERole;
import com.lovansom.carbookingapp.model.Role;
import com.lovansom.carbookingapp.model.User;
import com.lovansom.carbookingapp.payload.request.LoginRequest;
import com.lovansom.carbookingapp.payload.request.SignupRequest;
import com.lovansom.carbookingapp.payload.response.MessageResponse;
import com.lovansom.carbookingapp.payload.response.UserInfoResponse;
import com.lovansom.carbookingapp.repository.RoleRepository;
import com.lovansom.carbookingapp.repository.UserRepository;
import com.lovansom.carbookingapp.security.jwt.JwtUtils;
import com.lovansom.carbookingapp.security.service.UserDetailsImpl;

import jakarta.validation.Valid;

@CrossOrigin(origins = "http://locaolhost:4200", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

		ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

		List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString()).body(
				new UserInfoResponse(userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Lỗi: Username đã tồn tại!"));
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity.badRequest().body(new MessageResponse("Lỗi: Email đã được sử dụng!"));
		}

		// Create new user's account
		User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
				encoder.encode(signUpRequest.getPassword()));

		Set<String> strRoles = signUpRequest.getRole();
		Set<Role> roles = new HashSet<>();

		if (strRoles == null) {
			Role userRole = roleRepository.findByName(ERole.ROLE_USER)
					.orElseThrow(() -> new RuntimeException("Lỗi: Vai trò người dùng ko hợp lệ."));
			roles.add(userRole);
		} else {
			strRoles.forEach(role -> {
				switch (role) {
				case "admin":
					Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
							.orElseThrow(() -> new RuntimeException("Lỗi: Vai trò người dùng ko hợp lệ"));
					roles.add(adminRole);

					break;
				case "owner":
					Role modRole = roleRepository.findByName(ERole.ROLE_OWNER)
							.orElseThrow(() -> new RuntimeException("Lỗi: Vai trò người dùng ko hợp lệ"));
					roles.add(modRole);

					break;
				default:
					Role userRole = roleRepository.findByName(ERole.ROLE_USER)
							.orElseThrow(() -> new RuntimeException("Lỗi: Vai trò người dùng ko hợp lệ"));
					roles.add(userRole);
				}
			});
		}

		user.setRoles(roles);
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("Chúc mừng! Bạn đã đăng ký thành công!"));
	}

	  @PostMapping("/signout")
	  public ResponseEntity<?> logoutUser() {
	    ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
	    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
	        .body(new MessageResponse("Bạn đã đăng xuất thành công!"));
	  }

}
