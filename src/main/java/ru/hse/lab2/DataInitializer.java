package ru.hse.lab2;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.User;
import ru.hse.lab2.entity.Phone;  // ← ДОБАВИТЬ
import ru.hse.lab2.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            // Создаём пользователя с телефонами
            User user1 = new User("Ivan", "Ivanov", "ivan@test.ru");
            user1.getPhones().add(new Phone("+79991234567", user1));
            user1.getPhones().add(new Phone("+79997654321", user1));
            userRepository.save(user1);

            User user2 = new User("Maria", "Petrova", "maria@test.ru");
            user2.getPhones().add(new Phone("+79991112233", user2));
            userRepository.save(user2);

            User user3 = new User("Alex", "Sidorov", "alex@test.ru");
            // У Алекса пока нет телефонов
            userRepository.save(user3);

            System.out.println("✅ Тестовые данные успешно добавлены в БД!");
            System.out.println("📱 Создано пользователей: 3");
            System.out.println("📞 Создано телефонов: 3");
        } else {
            System.out.println("ℹ️ База данных уже содержит данные, пропускаем.");
        }
    }
}