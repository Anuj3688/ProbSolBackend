package com.probsol.init;

import com.probsol.entity.Entry;
import com.probsol.entity.Tag;
import com.probsol.entity.User;
import com.probsol.repository.EntryRepository;
import com.probsol.repository.UserRepository;
import com.probsol.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final EntryRepository entryRepository;
    private final TagService tagService;
    private final PasswordEncoder passwordEncoder;

    @Value("${probsol.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${probsol.seed.email:anuj@probsol.dev}")
    private String seedEmail;

    @Value("${probsol.seed.password:Password123!}")
    private String seedPassword;

    @Value("${probsol.seed.display-name:Anuj Tiwari}")
    private String seedDisplayName;

    public DataInitializer(UserRepository userRepository,
                           EntryRepository entryRepository,
                           TagService tagService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.entryRepository = entryRepository;
        this.tagService = tagService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        String email = seedEmail.trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            logger.info("Seeding initial personal account: {}", email);
            User newUser = new User(email, passwordEncoder.encode(seedPassword), seedDisplayName);
            return userRepository.save(newUser);
        });

        long existingCount = entryRepository.countByUserIdAndDeletedAtIsNull(user.getId());
        if (existingCount == 0) {
            logger.info("Seeding sample problems and solutions for {}", email);
            createSeedEntry(user, "problem", "OPEN",
                    "Need a better retry architecture for RabbitMQ consumers",
                    "Message reprocessing causes duplicate side effects on high lag.",
                    List.of("backend", "messaging", "rabbitmq"));

            createSeedEntry(user, "problem", "SOLVED",
                    "Memory leak in React 19 useEffect subscription",
                    "Event listeners not detaching properly when unmounting quick navigation screens.",
                    List.of("frontend", "react"));

            createSeedEntry(user, "solution", "SOLVED",
                    "Idempotency keys using Redis SETNX",
                    "Attach unique request UUID to every incoming mutation. Reject duplicate transactions within 5 minute TTL.",
                    List.of("backend", "redis", "architecture"));

            createSeedEntry(user, "problem", "OPEN",
                    "Slow search queries on large note datasets",
                    "Full scans taking > 200ms without compound indexing on user_id and status.",
                    List.of("database", "backend"));

            createSeedEntry(user, "solution", "SOLVED",
                    "Optimistic UI locking for offline sync",
                    "Store client mutations in IndexedDB with monotonic version clocks. Reconcile upon reconnection.",
                    List.of("frontend", "architecture"));

            logger.info("Seed data initialized successfully for daily use!");
        }
    }

    private void createSeedEntry(User user, String type, String status, String title, String description, List<String> tagNames) {
        Entry entry = new Entry(user, type, status, title, description);
        Set<Tag> tags = tagService.getOrCreateTags(user, tagNames);
        entry.setTags(tags);
        entryRepository.save(entry);
    }
}
