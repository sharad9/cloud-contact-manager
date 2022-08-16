package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;

	
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME"+userName);
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER" + user);
		model.addAttribute("user", user);
	}
	
	
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	@PostMapping("/process-contact")
	public String pressContact(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file,Principal principal,HttpSession session) {
		try {
			String name = principal.getName();
			User user = userRepository.getUserByUserName(name);
			
			
			if(file.isEmpty()) {
				System.out.println("File is empty");
				contact.setImage("contact.png");
			}else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
			}
			user.getContacts().add(contact);
			contact.setUser(user);
			
			userRepository.save(user);
			session.setAttribute("message", new Message("Your contact is added !! Add more..","success"));
			System.out.println("DATA "+contact);
		}catch(Exception e){
			System.out.println("ERROR "+e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong!! ..try again..","danger"));
			
		}
		
		
		return "normal/add_contact_form";
	}
	
	@GetMapping("/show_contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model model,Principal principal) {
		model.addAttribute("title","Show User Contacts");
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		Pageable pageable = PageRequest.of(page, 1);
		Page<Contact> contacts =  contactRepository.findContactsByUser(user.getId(),pageable);
		model.addAttribute("contacts",contacts);
		model.addAttribute("currentPage",page);
		model.addAttribute("totalPages",contacts.getTotalPages());
		return "normal/show_contacts";
	}
	
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId")Integer cId, Model model,Principal principal){
		System.out.println("CID "+cId);
		Optional<Contact> contactOptional =  contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		if(user.getId() == contact.getUser().getId()) {
			
			
			model.addAttribute("contact",contact);
			model.addAttribute("title",contact.getName());
		}
		
		
		
		return "normal/contact_detail";
	}
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid")Integer cId,Model model,Principal principal,HttpSession session) {
		Optional<Contact> contactOptional = contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);
		
		if(user.getId() == contact.getUser().getId()) {
			user.getContacts().remove(contact);
			userRepository.save(user);
			contact.setUser(null);
			contactRepository.delete(contact);
			session.setAttribute("message", new Message("Contact deleted successfully..","success"));
			
		}
		return "redirect:/user/show_contacts/0";
		
	}
	
	@PostMapping("/update_contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid,Model model) {
		model.addAttribute("title", "Update Contact");
		Contact contact =  contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);
		return "normal/update_form";
	}
	
	@RequestMapping(value="/process-update",method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file, Model model,Principal principal, HttpSession session) {
		
		Contact oldContactDetail = contactRepository.findById(contact.getcId()).get();
		try {
			File deleteFile = new ClassPathResource("static/img").getFile();
			File file1 = new File(deleteFile, oldContactDetail.getImage());
			file1.delete();
			
			if(! file.isEmpty()) {
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
			}else {
				contact.setImage(oldContactDetail.getImage());
			}
			User user = userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is update","success"));
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user"+contact.getcId()+"/contact";
		
	}
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title","Profile Page");
		return "normal/profile";
		
	}
}
