# Java Blockchain Implementation

A simple Java-based blockchain implementation with transaction support and proof-of-work consensus mechanism.

<br/>

### Overview

- This project demonstrates core blockchain concepts including:
- Block creation and mining with adjustable difficulty
- Transaction processing
- Chain validation
- Data persistence (currently file-based)

<br/>

### Features
- Proof-of-Work mining algorithm
- Transaction-based data structure
- Chain integrity validation
- JSON-based blockchain persistence

<br/>

### Technologies
- Java
- Maven
- Lombok
- Google Gson for JSON serialization
- SLF4J for logging

<br/>

### Current Implementation

The blockchain currently:

- Creates and mines blocks with transactions
- Uses SHA-256 hashing algorithm
- Validates blockchain integrity
- Persists data to JSON files in src/main/resources/data/

<br/>

### Mitigation to Database Storage

<br/>

##### Step 1: Add dependencies

- Add spring boot framework and database (probably postgresql) dependencies to pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

<br/>

##### Step 2: Create Database Entities

Example of a simple block entity:

```java
@Entity
@Table(name = "blocks")
public class BlockEntity {
    @Id
    private String hash;
    private String previousHash;
    @Column(length = 10000)
    private String data;
    private long timeStamp;
    private int nonce;

    // Getters, setters, constructors
}
```

<br/>

##### Step 3: Create Repository Interfaces

Example repository interface from Blockrepository:

```java
    public interface BlockRepository extends JpaRepository<BlockEntity, String> {
        List<BlockEntity> findAllByOrderByTimeStampAsc();
    }
```

<br/>

##### Step 4: Create Service Class

Example of Block service class:

```java
@Service
public class BlockchainService {
    @Autowired
    private BlockRepository blockRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    // Methods for saving/loading blockchain
    public void saveBlock(Block block) {
   
    }

    public List<Block> loadBlockchain() {
        // Load blocks from DB and reconstruct chain
    }
}
```

<br/>

##### Step 5: Update Application to Use Service

Replace current file operations in Main.java and BlockchainSave.java with calls to the new services.

<br/>

### If you want to replicate project here is how to get started

```bash
git clone 
cd blockchain
mvn clean install
java -jar target/blockchain-version1.jar
```