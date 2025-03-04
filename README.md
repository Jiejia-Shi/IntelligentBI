# IntelligentBI

IntelligentBI is an advanced business intelligence (BI) system powered by the OpenAI API. It enables users to perform "one-click data analysis", simplifying complex data insights with AI-driven automation.

Key technologies: Java, React, OpenAI, Spring Boot, Mybatis, MySQL, AWS, Redis, RabbitMQ

<img width="400" alt="216511505159d677a7f0cf3fecc27b4" src="https://github.com/user-attachments/assets/a18c877d-7b07-4bb2-bb68-eee1cf36702b" />

<img width="400" alt="78ae494010052f2f3da641b9d7372d3" src="https://github.com/user-attachments/assets/2eca0f98-1407-44f4-be49-9b20bdc6da71" />

# How to use it
1. Choose analysis mode (**Sync** or **Async**)

2. Add analysis request and upload data

3. Check the result (If you chose **Async Analysis**, visit the **My Chart** page to view the results.)


# Installation
Follow these steps to install and set up IntelligentBI:

1. Clone the IntelligentBI project.

2. Ensure MySQL, Redis and RabbitMQ are installed and running, configure them in yml files.

3. Configure your OpenAI Key as an environment variable:

    3.1 Windows: setx OPENAI_API_KEY "your-secret-key"

    3.2 Mac/Linux: export OPENAI_API_KEY="your-secret-key"

4. Initialize the Database. Run the SQL script to create the required tables (ibi-backend/sql/create_table.sql).

5. Start the project.
