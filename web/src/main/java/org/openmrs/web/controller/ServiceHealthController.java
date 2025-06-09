package org.openmrs.web.indicationController;

import org.openmrs.web.Listener;
import org.openmrs.web.filter.initialization.InitializationFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndicatorController{
	
	@GetMapping("/liveness")
	public ResponseEntity isOpenmrsLive(){
		boolean isOpenmrsLive=InitializationFilter.isInstallationStarted() || Listener.isOpenmrsStarted();
		return isOpenmrsLive?new ResponseEntity(HttpStatus.OK):new ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE);
	}
	
	@GetMapping("/readiness")
	public ResponseEntity isOpenmrsReady(){
		boolean isOpenmrsReady = Listener.isOpenmrsStarted();
		return isOpenmrsReady?new ResponseEntity(HttpStatus.OK):new ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE);
	}
	
}
