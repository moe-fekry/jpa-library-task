# JPA Library Management System

A small JPA/Hibernate project built with H2 to practice entity relationships, fetching,
cascading, inheritance, JPQL/HQL, and the Criteria API.

## Stack

- Java 17
- Hibernate ORM 6.4 (JPA 3.0 provider)
- H2 in-memory database
- Maven

## Running

```
mvn compile exec:java
```

This runs `Main`, which inserts sample data, then calls into `QueryDemo` to run every
Task 2 query. `hibernate.show_sql` is enabled, so the generated SQL and DDL print to the
console as everything executes.

## Domain model

- **Author (1) — (N) Book**: bidirectional, `Book` is the owning side and stores the FK
  as `author_id`. `Author.books` uses `mappedBy = "author"`, `cascade = ALL`, and
  `orphanRemoval = true`. `Author.addBook()` / `removeBook()` keep both sides in sync.
- **Publisher (1) — (N) Book**: unidirectional `@ManyToOne` from `Book` to `Publisher`
  (Publisher also exposes an inverse `books` list for convenience, but the relationship
  is only required in the `Book -> Publisher` direction).
- **Book (N) — (N) Category**: `@ManyToMany` owned by `Book` via the join table
  `book_category` (`book_id`, `category_id`). `Category` is the inverse side
  (`mappedBy = "categories"`).
- **Person (inheritance root) -> Employee / Customer**: `JOINED` inheritance strategy.

## Owning vs inverse sides

| Relationship | Owning side | Foreign key location |
|---|---|---|
| Author <-> Book | Book | `book.author_id` |
| Book -> Publisher | Book | `book.publisher_id` |
| Book <-> Category | Book | join table `book_category` |

The owning side is whichever entity does **not** declare `mappedBy` — it's the side JPA
looks at to decide what to write to the foreign key / join table. `Author.books` and
`Category.books` both declare `mappedBy`, which is why they are the inverse sides and
changes made only on those collections (without also updating the owning side) would
not be persisted.

## Fetch choices

- All collections (`Author.books`, `Publisher.books`, `Category.books`,
  `Book.categories`) are `FetchType.LAZY` — the JPA default for collections, made
  explicit here on purpose so the choice is visible in the mapping.
- `Book.author` is explicitly set to `FetchType.LAZY`, overriding the JPA default of
  `EAGER` for `@ManyToOne`. This avoids loading an author every time a book is fetched
  when it isn't needed.
- `Book.publisher` is explicitly set to `FetchType.EAGER`, since publisher is small,
  rarely null, and commonly displayed alongside a book — a case where paying for the
  extra join upfront is worth it instead of relying on the implicit default.

## Cascade and orphan removal

`Author.books` uses `CascadeType.ALL` plus `orphanRemoval = true`:

- Persisting a new `Author` with new `Book`s in its collection persists the books too,
  without calling `persist()` on each book separately (demonstrated in `Main`).
- Removing a `Book` from `Author.books` (via `Author.removeBook()`) deletes that book
  row from the database on flush, even though `remove()` was never called on the
  `Book` directly (also demonstrated in `Main`).

`Book.categories` and `Publisher.books` do not cascade — deleting an author's book
should not delete a shared category or a publisher, and removing a book from a
category's in-memory set should not delete the category.

## Inheritance strategy

`Person` uses `InheritanceType.JOINED`: one `person` table with the shared columns
(`id`, `name`, `email`), and separate `employee` / `customer` tables holding only their
own columns, joined back to `person` by primary key.

**Why JOINED over the alternatives:**

- **SINGLE_TABLE** would need one wide table with nullable columns for every subclass
  field (`salary`, `department`, `address`, `phone`), and a discriminator column. It's
  the fastest to query (no joins) but sacrifices normalization and adds NOT NULL
  constraints that can't be enforced at the database level for subclass-specific
  columns.
- **TABLE_PER_CLASS** duplicates the shared `name`/`email` columns into both `employee`
  and `customer`, and polymorphic queries across `Person` need a `UNION` under the
  hood, which is generally the least efficient option and complicates generated IDs.
- **JOINED** was chosen because this schema benefits from proper normalization and
  referential integrity — shared person data lives in exactly one place — and the
  extra join cost is negligible at this scale. It also keeps each table's columns
  strictly NOT NULL-able only where that subclass actually needs them.

## Query decisions (Task 2)

- **`booksByAuthorName` / `booksByPublisher`**: simple JPQL path expressions
  (`b.author.name`, `b.publisher.name`) — since `Book.author` and `Book.publisher` are
  `@ManyToOne`, JPQL can navigate the association directly without an explicit `JOIN`.
- **`findBookById`**: uses a positional parameter (`?1`) instead of a named one, to
  show both parameter-binding styles are supported.
- **`authorWithBooksJoinFetch`**: `JOIN FETCH` on `a.books` loads the author and its
  books in a single query/round trip and keeps the collection initialized after the
  persistence context closes — the plain `LAZY` version further down throws a lazy
  exception once its `EntityManager` is closed before the collection is touched,
  because there is no active session left to run the extra `SELECT`.
- **Aggregate query**: `LEFT JOIN a.books b ... GROUP BY a.name` (rather than an inner
  `JOIN`) so authors with zero books still appear in the result with a count of 0.
- **Criteria API (`findBooksByTitle`)**: minimal example — `Root<Book>` +
  `cb.equal(...)`.
- **Criteria API (`findBooksDynamic`)**: builds a `List<Predicate>` and only adds a
  predicate when its corresponding argument is non-blank, so the same method serves
  "title only", "author only", "both", or "neither" (returns everything) without four
  separate query methods.
- **Standard JPQL vs Hibernate HQL**: the standard-JPQL query runs through
  `EntityManager.createQuery(...)`. The HQL version unwraps the JPA `EntityManager`
  into a native Hibernate `Session` and uses the `FROM`-only shorthand (no `SELECT`
  required) along with `setCacheable(true)` — an option that only exists on Hibernate's
  own `Query`/`Session` API, not on the plain `jakarta.persistence.Query` interface.

## Optional extension (orm.xml)

Not included in this deliverable. To do it: remove the `@OneToMany`/`@ManyToOne`
annotations from `Author`/`Book`, add an `orm.xml` under `src/main/resources/META-INF`
that declares the same `author`/`books` mapping in XML, and reference it from
`persistence.xml` with a `<mapping-file>META-INF/orm.xml</mapping-file>` entry. The
generated schema and query behavior stay identical since XML and annotation mappings
are equivalent inputs to the same Hibernate metadata model.
