package com.bitespeed.identityreconcilation.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.bitespeed.identityreconcilation.entity.Contact;
import com.bitespeed.identityreconcilation.service.ContactService;

@RestController
public class ContactController {
	
	 private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

	@Autowired
	private ContactService contactService;

	@PostMapping("/identify")
	public ResponseEntity<Object> identifyContact(@RequestBody IdentifyRequest request) {
		logger.info("Request Received-Identity Reconcilation Process-Started!!");
		Contact contact = contactService.identifyOrCreateContact(request.getEmail(), request.getPhoneNumber());

		Map<String, Object> contactInfo = new LinkedHashMap<>();
		contactInfo.put("primaryContactId", contactService.findPrimaryContactId(contact));
		contactInfo.put("emails", contactService.getEmails(contact));
		contactInfo.put("phoneNumbers", contactService.getPhoneNumbers(contact));
		contactInfo.put("secondaryContactIds", contactService.getSecondaryContactIds(contact));

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("contact", contactInfo);

		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
