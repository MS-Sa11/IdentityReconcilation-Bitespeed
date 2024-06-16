package com.bitespeed.identityreconcilation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bitespeed.identityreconcilation.entity.Contact;
import com.bitespeed.identityreconcilation.entity.LinkPrecedence;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

	List<Contact> findByEmailOrPhoneNumber(String email, String phoneNumber);

	List<Contact> findByLinkedId(Long id);

	List<Contact> findByLinkedIdAndLinkPrecedence(Long id, LinkPrecedence secondary);

}
