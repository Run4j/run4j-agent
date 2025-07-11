# Run4j Agent

This project is a Java application manager that:

- Starts Java JAR files in isolated Podman containers
- Stops running Java applications
- Creates PostgreSQL databases for each application
- Monitors application status
- Sets memory and CPU limits per application

Basically, it's a hosting platform for Java applications with automatic database provisioning.
