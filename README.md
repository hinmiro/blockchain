# Java Blockchain Implementation

A simple Java-based blockchain implementation with transaction support and proof-of-work consensus mechanism.

<br/>
<br/>

#### Overview

- This project demonstrates core blockchain concepts including:
- Block creation and mining with adjustable difficulty
- Transaction processing
- Chain validation
- Data persistence (currently file-based)

<br/>
<br/>

#### Features
- Proof-of-Work mining algorithm
- Transaction-based data structure
- Chain integrity validation
- JSON-based blockchain persistence

<br/>
<br/>

#### Technologies
- Java
- Maven
- Lombok
- Google Gson for JSON serialization
- SLF4J for logging

<br/>
<br/>

#### Current Implementation

The blockchain currently:

- Creates and mines blocks with transactions
- Uses SHA-256 hashing algorithm
- Validates blockchain integrity
- Persists data to JSON files in src/main/resources/data/

#### If you want to replicate project here is how to get started

```
git clone 
cd blockchain
mvn clean install
java -jar target/blockchain-version1.jar
```