package com.bitespeed.identityreconcilation.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bitespeed.identityreconcilation.controller.ContactController;
import com.bitespeed.identityreconcilation.entity.Contact;
import com.bitespeed.identityreconcilation.entity.LinkPrecedence;
import com.bitespeed.identityreconcilation.repository.ContactRepository;

@Service
public class ContactService {

	private static final Logger logger = LoggerFactory.getLogger(ContactService.class);
	
	@Autowired
	private ContactRepository contactRepository;

	@Transactional
	public Contact identifyOrCreateContact(String email, String phoneNumber) {
		logger.info("Identity checking started in DB using email and PhoneNumber");
		List<Contact> existingContacts = contactRepository.findByEmailOrPhoneNumber(email, phoneNumber);

		if (existingContacts.isEmpty()) {
			logger.info("Adding entry in DB-Creating PRIMARY contact");
			Contact newContact = new Contact();
			newContact.setEmail(email);
			newContact.setPhoneNumber(phoneNumber);
			newContact.setLinkPrecedence(LinkPrecedence.PRIMARY);
			newContact.setCreatedAt(LocalDateTime.now());
			return contactRepository.save(newContact);
		} else {
			logger.info("Checking for exact match in DB");
			for (Contact existingContact : existingContacts) {
				if (Objects.equals(existingContact.getEmail(), email) &&
						Objects.equals(existingContact.getPhoneNumber(), phoneNumber)) {
					logger.info("Exact match found- Returning existing Contact");
					return existingContact;
				}
			}

			logger.info("Finding the primary contact in DB for received existing contact");
			Contact primaryContact = findPrimaryContact(existingContacts);

			if (primaryContact == null) {
				logger.info("Primary contact is null. So existing contacts are secondary");
				// Choose the oldest created contact as the primary
				primaryContact = findOldestCreatedContact(existingContacts);
				primaryContact.setLinkPrecedence(LinkPrecedence.PRIMARY);
				primaryContact.setUpdatedAt(LocalDateTime.now());
				primaryContact = contactRepository.save(primaryContact);
			} else {
				for (Contact existingContact : existingContacts) {
					if (existingContact.getLinkPrecedence() == LinkPrecedence.PRIMARY &&
							!existingContact.getId().equals(primaryContact.getId())) {
						logger.info("Converting Primary contact as Secondary Contact");
						existingContact.setLinkPrecedence(LinkPrecedence.SECONDARY);
						existingContact.setLinkedId(primaryContact.getId());
						existingContact.setUpdatedAt(LocalDateTime.now());
						contactRepository.save(existingContact);
						// Return primaryContact as it remains the primary
						return primaryContact;
					}
				}
			}	
			logger.info("Create a new secondary contact linked to the identified primary contact");
			return createNewSecondaryContact(primaryContact, email, phoneNumber);
		}
	}

	private Contact findOldestCreatedContact(List<Contact> contacts) {
		Contact oldestContact = null;
		for (Contact contact : contacts) {
			if (oldestContact == null || contact.getCreatedAt().isBefore(oldestContact.getCreatedAt())) {
				oldestContact = contact;
			}
		}
		return oldestContact;
	}

	private Contact findPrimaryContact(List<Contact> contacts) {
		for (Contact contact : contacts) {
			if (contact.getLinkPrecedence() == LinkPrecedence.PRIMARY) {
				return contact;
			}
		}
		return null;
	}

	private Contact createNewSecondaryContact(Contact primaryContact, String email, String phoneNumber) {
		logger.info("Adding entry in DB-Creating SECONDARY contact");
		Contact contact = new Contact();
		contact.setEmail(email);
		contact.setPhoneNumber(phoneNumber);
		contact.setLinkPrecedence(LinkPrecedence.SECONDARY);
		contact.setLinkedId(primaryContact.getId());
		contact.setCreatedAt(LocalDateTime.now());
		return contactRepository.save(contact);
	}

	public List<String> getEmails(Contact contact) {
		Set<String> emails = new HashSet<>();
		Contact primaryContact = getPrimaryContact(contact);

		emails.add(primaryContact.getEmail());

		List<Contact> secondaryContacts = contactRepository.findByLinkedIdAndLinkPrecedence(primaryContact.getId(), LinkPrecedence.SECONDARY);
		for (Contact secondaryContact : secondaryContacts) {
			emails.add(secondaryContact.getEmail());
		}

		return new ArrayList<>(emails);
	}

	public List<String> getPhoneNumbers(Contact contact) {
		Set<String> phoneNumbers = new HashSet<>();
		Contact primaryContact = getPrimaryContact(contact);

		phoneNumbers.add(primaryContact.getPhoneNumber());

		List<Contact> secondaryContacts = contactRepository.findByLinkedIdAndLinkPrecedence(primaryContact.getId(), LinkPrecedence.SECONDARY);
		for (Contact secondaryContact : secondaryContacts) {
			phoneNumbers.add(secondaryContact.getPhoneNumber());
		}

		return new ArrayList<>(phoneNumbers);
	}

	public List<Long> getSecondaryContactIds(Contact contact) {
		List<Long> secondaryContactIds = new ArrayList<>();
		Contact primaryContact = getPrimaryContact(contact);

		List<Contact> secondaryContacts = contactRepository.findByLinkedIdAndLinkPrecedence(primaryContact.getId(), LinkPrecedence.SECONDARY);
		for (Contact secondaryContact : secondaryContacts) {
			secondaryContactIds.add(secondaryContact.getId());
		}

		return secondaryContactIds;
	}

	private Contact getPrimaryContact(Contact contact) {
		if (contact.getLinkPrecedence() == LinkPrecedence.SECONDARY) {
			return contactRepository.findById(contact.getLinkedId()).orElse(contact);
		}
		return contact;
	}

	public Long findPrimaryContactId(Contact contact) {
		return getPrimaryContact(contact).getId();
	}
}
