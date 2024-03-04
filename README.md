# Configuration Library

This library provides a flexible and powerful configuration solution for Java applications, supporting loading from files or Consul, with configurations formatted in YAML. It features value substitution with system properties or environment variables, and the ability to stream configurations to JSON. For Consul configurations, it intelligently walks up the key namespace to find default configurations, ensuring your application can always find the most relevant settings.

## Features

- **Load Configurations**: Easily load configuration settings from a file system or Consul.
- **YAML Format**: Configuration files are written in YAML, a human-readable data serialization standard.
- **Value Substitution**: Supports dynamic value substitution within your YAML files using `{{ }}` for system properties or environment variables.
- **Native Types**: Supports native types like, long, String, float as well as size parameters, K, M, T etc
- **Consul Key Namespace Walking**: For Consul configurations, the library will search up the key namespace for a "default" configuration file if the specific configuration is not found, following a predetermined path until a default is located or falling back on built-in defaults.

## Getting Started

### Prerequisites

Before you begin, ensure you have the latest version of Java installed on your development machine.

### Installation

To use the configuration library in your project, add the following dependency to your `pom.xml` for Maven:

```xml
        <dependency>
          <groupId>io.mapsmessaging</groupId>
          <artifactId>io.mapsmessaging</artifactId>
          <version>1.0.0-SNAPSHOT</version>
        </dependency>
```

For Gradle, add this to your build.gradle:
```groovy
dependencies {
    implementation 'io.mapsmessaging:io.mapsmessaging:1.0.0-SNAPSHOT'
}
```
### Basic Usage

To load a configuration from a YAML file:

``` java
PropertyManager propertyManager = new FileYamlPropertyManager("path/to/your/config_directory/");
```

To load from Consul with namespace walking:

``` java
  ConsulManagerFactory.getInstance().start("/oceania/aus/syd/server_name");
  ConsulManagerFactory.getInstance().getManager().getProperties();
```

### Value Substitution

To use value substitution, format your YAML file like so:

``` YAML 
database:
  host: {{DB_HOST}}
  port: {{DB_PORT}}
```
