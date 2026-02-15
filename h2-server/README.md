# H2 TCP Server

Starts the shared H2 database in TCP server mode on port **9092**. All services (restaurant, order, identity, delivery) connect to this single database using separate schemas.

**Start this first** before running any of the other services.

```bash
# From project root
./restaurant-service/mvnw -f h2-server/pom.xml spring-boot:run
```

Or from the `h2-server` directory:

```bash
mvn spring-boot:run
```

The database file is created at `./data/foodplatform` relative to the working directory where the server is started. Use the same working directory (e.g. project root) when starting the server and when connecting from services if you need to reuse an existing database file.
