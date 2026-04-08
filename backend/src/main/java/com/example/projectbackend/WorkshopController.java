package com.example.projectbackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.Valid;

import java.util.*;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
@RequestMapping("/api")
public class WorkshopController {

    @Autowired
    private WorkshopRepository repo;

    @Autowired
    private JoinWorkshopRepository joinRepo;

    @Autowired
    private EmailService emailService;

    // ✅ GET ALL WORKSHOPS
    @GetMapping("/workshops")
    public List<Workshop> getAll() {
        return repo.findAll();
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors()
                            .stream()
                            .map(err -> err.getDefaultMessage())
                            .findFirst()
                            .orElse("Invalid input");
        return ResponseEntity.badRequest().body(errorMsg);
    }
    @Email(message = "Invalid email format")
    @Pattern(
        regexp = "^[A-Za-z0-9+_.-]+@gmail\\.com$",
        message = "Only Gmail allowed"
    )
    @NotNull
    private String email;
    
    // ✅ GET WORKSHOP BY ID
    @GetMapping("/workshops/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Workshop> workshop = repo.findById(id);

        if (workshop.isPresent()) {
            return ResponseEntity.ok(workshop.get());
        } else {
            return ResponseEntity.status(404).body("Workshop not found");
        }
    }

    // ✅ ADD WORKSHOP (Teacher)
    @PostMapping("/workshops")
    public ResponseEntity<?> addWorkshop(@RequestBody Workshop workshop) {
        try {
            Workshop saved = repo.save(workshop);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving workshop");
        }
    }
    
 // Delete a workshop by ID
    @DeleteMapping("/workshops/{id}")
    public ResponseEntity<?> deleteWorkshop(@PathVariable Long id) {
        Optional<Workshop> ws = repo.findById(id);
        if (ws.isEmpty()) {
            return ResponseEntity.status(404).body("Workshop not found");
        }
        repo.deleteById(id);
        return ResponseEntity.ok("Workshop deleted successfully");
    }
    
    

    // ✅ JOIN WORKSHOP (Student) — UPDATED
    @PostMapping("/join")
    public ResponseEntity<?> joinWorkshop(@Valid @RequestBody JoinRequest req) {

        try {
            // 🔒 Check already joined
            if (joinRepo.existsByWorkshopIdAndEmail(req.getWorkshopId(), req.getEmail())) {
                return ResponseEntity.ok("Already Joined");
            }

            // 🔍 Check workshop exists
            Optional<Workshop> optionalWs = repo.findById(req.getWorkshopId());
            if (optionalWs.isEmpty()) {
                return ResponseEntity.status(404).body("Workshop not found");
            }

            Workshop ws = optionalWs.get();

            // 💾 Save in DB
            JoinWorkshop join = new JoinWorkshop();
            join.setWorkshopId(ws.getId());
            join.setEmail(req.getEmail());
            joinRepo.save(join);

            // 📩 Send Email
            String subject = "Workshop Confirmation: " + ws.getTitle();
            String message = "You have successfully joined the workshop.\n\n"
                    + "Title: " + ws.getTitle() + "\n"
                    + "Zoom Link: " + ws.getZoomLink();

            emailService.sendEmail(req.getEmail(), subject, message);

            return ResponseEntity.ok("Joined successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
