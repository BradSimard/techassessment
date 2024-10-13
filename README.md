# ThinkOn Tech Assessment

## Setup
1. Create a database named ```thinkon``` in your MariaDB server
2. Run the following query to create the required schema
```sql
CREATE TABLE thinkon.`user` (
	id uuid DEFAULT SYS_GUID() NOT NULL,
	username varchar(100) NOT NULL,
	firstname varchar(100) NOT NULL,
	lastname varchar(100) NOT NULL,
	email varchar(100) NOT NULL,
	phone varchar(100) DEFAULT "" NOT NULL,
	CONSTRAINT user_pk PRIMARY KEY (id),
	CONSTRAINT user_unique UNIQUE KEY (username),
	CONSTRAINT user_unique_1 UNIQUE KEY (email)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4;
```
3. You'll need to setup your DB connection details. You can do this one of two ways:
 
### Method #1: Environment variables
```DB_USER```: your username

```DB_PASSWORD```: your password

```DB_CONNECTION_URL```: ```jdbc:mariadb://localhost:3306/thinkon``` (probably)

### Method #2: Edit the fallbacks in the code
Open the file: ```tech/simard/thinkon/db/DBConnection.java```

Modify the ```orElse``` values seen from lines 12 - 14:

```java
String username = Optional.ofNullable(System.getenv("DB_USER")).orElse("*YOUR USERNAME*");
String password = Optional.ofNullable(System.getenv("DB_PASSWORD")).orElse("*YOUR PASSWORD*");
String connectionUrl = Optional.ofNullable(System.getenv("DB_CONNECTION_URL")).orElse("*YOUR CONNECTION URL*");
```
