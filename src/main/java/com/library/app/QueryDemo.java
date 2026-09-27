package com.library.app;

import com.library.entity.Author;
import com.library.entity.Book;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

public class QueryDemo {

    public static void run(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();

        System.out.println("--- Books by author name (JPQL) ---");
        TypedQuery<Book> q1 = em.createQuery(
                "SELECT b FROM Book b WHERE b.author.name = :authorName", Book.class);
        q1.setParameter("authorName", "George Orwell");
        q1.getResultList().forEach(System.out::println);

        System.out.println();
        System.out.println("--- Books by publisher (JPQL) ---");
        TypedQuery<Book> q2 = em.createQuery(
                "SELECT b FROM Book b WHERE b.publisher.name = :publisherName", Book.class);
        q2.setParameter("publisherName", "Penguin Books");
        q2.getResultList().forEach(System.out::println);

        System.out.println();
        System.out.println("--- Book by id with positional parameter (JPQL) ---");
        TypedQuery<Book> q3 = em.createQuery(
                "SELECT b FROM Book b WHERE b.id = ?1", Book.class);
        q3.setParameter(1, 1L);
        List<Book> byIdResult = q3.getResultList();
        System.out.println(byIdResult.isEmpty() ? "No book with id=1" : byIdResult.get(0));

        System.out.println();
        System.out.println("--- Author with books via JOIN FETCH ---");
        TypedQuery<Author> q4 = em.createQuery(
                "SELECT DISTINCT a FROM Author a JOIN FETCH a.books WHERE a.name = :authorName", Author.class);
        q4.setParameter("authorName", "George Orwell");
        Author fetchedAuthor = q4.getSingleResult();
        System.out.println(fetchedAuthor.getName() + " -> " + fetchedAuthor.getBooks());

        System.out.println();
        System.out.println("--- Comparing plain LAZY query vs JOIN FETCH ---");
        EntityManager emCompare = emf.createEntityManager();
        TypedQuery<Author> lazyQuery = emCompare.createQuery(
                "SELECT a FROM Author a WHERE a.name = :authorName", Author.class);
        lazyQuery.setParameter("authorName", "George Orwell");
        Author lazyAuthor = lazyQuery.getSingleResult();
        emCompare.close();
        try {
            System.out.println("Accessing books after close (LAZY): " + lazyAuthor.getBooks().size());
        } catch (Exception e) {
            System.out.println("LAZY query without JOIN FETCH threw " + e.getClass().getSimpleName()
                    + " because the session used to load the collection was already closed");
        }
        System.out.println("JOIN FETCH avoided this because it loaded the collection eagerly in the same query, "
                + "within the still-open persistence context");

        System.out.println();
        System.out.println("--- Aggregate query: book count per author (JPQL GROUP BY) ---");
        TypedQuery<Object[]> q5 = em.createQuery(
                "SELECT a.name, COUNT(b) FROM Author a LEFT JOIN a.books b GROUP BY a.name", Object[].class);
        for (Object[] row : q5.getResultList()) {
            System.out.println(row[0] + " -> " + row[1] + " book(s)");
        }

        System.out.println();
        System.out.println("--- Criteria API: books by title ---");
        List<Book> criteriaResult = findBooksByTitle(em, "1984");
        criteriaResult.forEach(System.out::println);

        System.out.println();
        System.out.println("--- Criteria API: dynamic optional filters (title + author name) ---");
        System.out.println("Filtering by title only:");
        findBooksDynamic(em, "Clean", null).forEach(System.out::println);
        System.out.println("Filtering by author only:");
        findBooksDynamic(em, null, "Orwell").forEach(System.out::println);
        System.out.println("Filtering by both:");
        findBooksDynamic(em, "Animal", "Orwell").forEach(System.out::println);
        System.out.println("No filters (returns all books):");
        findBooksDynamic(em, null, null).forEach(System.out::println);

        System.out.println();
        System.out.println("--- Standard JPQL vs Hibernate-specific HQL feature ---");
        TypedQuery<Book> standardJpql = em.createQuery(
                "SELECT b FROM Book b WHERE b.title LIKE :pattern", Book.class);
        standardJpql.setParameter("pattern", "%Farm%");
        standardJpql.getResultList().forEach(System.out::println);

        Session session = em.unwrap(Session.class);
        List<Book> hqlCacheableResult = session.createQuery(
                        "FROM Book b WHERE b.title LIKE :pattern", Book.class)
                .setParameter("pattern", "%Farm%")
                .setCacheable(true)
                .list();
        System.out.println("HQL result via native Hibernate Session (supports FROM-only shorthand and "
                + "Hibernate-only options like setCacheable, unavailable on the plain JPA Query interface): "
                + hqlCacheableResult);

        em.close();
    }

    private static List<Book> findBooksByTitle(EntityManager em, String title) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book> root = cq.from(Book.class);
        cq.select(root).where(cb.equal(root.get("title"), title));
        return em.createQuery(cq).getResultList();
    }

    private static List<Book> findBooksDynamic(EntityManager em, String title, String authorName) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book> root = cq.from(Book.class);

        List<Predicate> predicates = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            predicates.add(cb.like(root.get("title"), "%" + title + "%"));
        }

        if (authorName != null && !authorName.isBlank()) {
            Join<Book, Author> authorJoin = root.join("author");
            predicates.add(cb.like(authorJoin.get("name"), "%" + authorName + "%"));
        }

        cq.select(root).distinct(true).where(predicates.toArray(new Predicate[0]));
        return em.createQuery(cq).getResultList();
    }
}
