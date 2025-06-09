package org.openmrs.web.controller;

import org.openmrs.web.Listener;
import org.openmrs.web.filter.initialization.InitializationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceHealthController {
	
	@GetMapping("/liveness")
	public ResponseEntity isOpenmrsLive(){
		boolean isOpenmrsLive= Listener.isSetupNeeded() || InitializationFilter.isInstallationStarted() || Listener.isOpenmrsStarted();
		return new ResponseEntity(isOpenmrsLive?HttpStatus.OK:HttpStatus.SERVICE_UNAVAILABLE);
	}
	
	@GetMapping("/readiness")
	public ResponseEntity isOpenmrsReady(){
		boolean isOpenmrsReady = Listener.isOpenmrsStarted();
		return new ResponseEntity(isOpenmrsReady?HttpStatus.OK:HttpStatus.SERVICE_UNAVAILABLE);
	}
	
}
