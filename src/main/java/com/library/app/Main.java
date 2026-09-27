package com.library.app;

import com.library.entity.Author;
import com.library.entity.Book;
import com.library.entity.Category;
import com.library.entity.Customer;
import com.library.entity.Employee;
import com.library.entity.Publisher;
import com.library.util.JpaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf = JpaUtil.getEntityManagerFactory();
        EntityManager em = emf.createEntityManager();

        Publisher penguin = new Publisher("Penguin Books");
        Publisher oreilly = new Publisher("O'Reilly Media");

        Category fiction = new Category("Fiction");
        Category tech = new Category("Technology");
        Category history = new Category("History");

        Author orwell = new Author("George Orwell");
        Author martin = new Author("Robert C. Martin");

        Book b1 = new Book("1984", "978-0451524935");
        b1.setPublisher(penguin);
        b1.addCategory(fiction);
        b1.addCategory(history);
        orwell.addBook(b1);

        Book b2 = new Book("Animal Farm", "978-0451526342");
        b2.setPublisher(penguin);
        b2.addCategory(fiction);
        orwell.addBook(b2);

        Book b3 = new Book("Clean Code", "978-0132350884");
        b3.setPublisher(oreilly);
        b3.addCategory(tech);
        martin.addBook(b3);

        Employee employee = new Employee("Sara Ahmed", "sara@library.com", 4200.0, "Circulation Desk");
        Customer customer = new Customer("Karim Youssef", "karim@example.com", "12 Nile St, Cairo", "01000000000");

        em.getTransaction().begin();
        em.persist(penguin);
        em.persist(oreilly);
        em.persist(fiction);
        em.persist(tech);
        em.persist(history);
        em.persist(orwell);
        em.persist(martin);
        em.persist(employee);
        em.persist(customer);
        em.getTransaction().commit();

        System.out.println("=== Sample data inserted ===");
        System.out.println("Author id=" + orwell.getId() + " has " + orwell.getBooks().size() + " books persisted through cascade");

        Long orwellId = orwell.getId();
        Long b2Id = b2.getId();

        System.out.println();
        System.out.println("=== Demonstrating orphanRemoval ===");
        em.getTransaction().begin();
        Author managedOrwell = em.find(Author.class, orwellId);
        Book bookToRemove = null;
        for (Book b : managedOrwell.getBooks()) {
            if (b.getId().equals(b2Id)) {
                bookToRemove = b;
            }
        }
        managedOrwell.removeBook(bookToRemove);
        em.getTransaction().commit();

        Book checkRemoved = em.find(Book.class, b2Id);
        System.out.println("Book 'Animal Farm' after removal from Author.books: " + checkRemoved);

        System.out.println();
        System.out.println("=== Demonstrating LazyInitializationException ===");
        EntityManager em2 = emf.createEntityManager();
        Author detachedAuthor = em2.find(Author.class, martin.getId());
        em2.close();
        try {
            System.out.println(detachedAuthor.getBooks().size());
        } catch (LazyInitializationException e) {
            System.out.println("Caught expected LazyInitializationException: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== Task 1 complete, running Task 2 query demos ===");
        System.out.println();

        QueryDemo.run(emf);

        em.close();
        JpaUtil.close();
    }
}
