![build-main](https://github.com/rsaestrela/vertx-java-ms/workflows/build-main/badge.svg)

# vertx-java-ms

A nonsense Java microservice that can be useful as a template, or a _how to get started_ project with Vert.x and PostgreSQL. The setup
includes the basic dependencies and mechanisms in place to develop and ship any modern Vert.x microservice, such as: 
 
* [Vert.x](https://github.com/eclipse-vertx/vert.x) (Core, Web, Client, Config, JDBC)
* [Vert.x jOOQ](https://github.com/jklingsporn/vertx-jooq) for _Vertx-ified_ DAOs and POJOs to execute CRUD-operations asynchronously
* [Google Guice](https://github.com/google/guice) for DI
* [Flyway](https://github.com/flyway/flyway) for database migrations
* `maven-shade-plugin` to create executable fat JARs (`maven-assembly-plugin` can be also used, though)
* GitHub Actions for CI/CD - Releasing, containerization and deployment workflows still TBD 🚧

Documentation TBC 🚀