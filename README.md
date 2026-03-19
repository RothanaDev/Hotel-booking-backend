
## 📖 About The Project

This project serves as the core backend engine for a fully-featured **Hotel Booking System**. It handles everything from user authentication and room management, to secure payment processing and automated notifications, ensuring a seamless experience for both hotel administrators and guests.

## ✨ Key Features

- 🔐 **Authentication & Security:** Robust JWT-based security using **Spring Security** and **OAuth2 Resource Server**.
- 💳 **Payment Processing:** Integrated with the **PayPal Checkout SDK** for secure, reliable transactions.
- 🏞️ **Media Management:** Seamless and fast image uploads directly to **Cloudinary**.
- 📬 **Real-time Notifications:** 
  - **Email:** Automated emails for booking confirmations and OTP verification via Spring Mail.
  - **Telegram:** Instant alerts and chat updates sent via a dedicated **Telegram Bot**.
- 🗄️ **Data Persistence:** Relational database management using **PostgreSQL** and **Spring Data JPA**.
- 🔄 **Efficient Mapping:** Clean separation of concerns with entity-DTO conversions handled by **MapStruct**.

## 💻 Tech Stack

| Category | Technologies |
| --- | --- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.9 |
| **Database** | PostgreSQL |
| **Build Tool** | Gradle |
| **Cloud Services** | Cloudinary (Images), PayPal (Payments) |
| **Libraries** | Lombok, MapStruct, Telegram Bots Starter |

## 🚀 Getting Started

Follow these steps to set up the project locally.

### Prerequisites

* **JDK 21** or higher
* **PostgreSQL** running locally or via Docker
* **Docker** & **Docker Compose** *(optional)*

### 1. Clone & Configure

First, clone the repository to your local machine. Next, you will need to set up your environment variables. 

Create a `.env` file in the root directory (you can use `.env.example` as a template) and fill in your credentials:

```ini
# PostgreSQL Setup
POSTGRES_DB=db_hotel_booking
POSTGRES_USER=hotel_booking
POSTGRES_PASSWORD=hotel_booking123

# Cloudinary Setup
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# PayPal Setup
PAYPAL_CLIENT_ID=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_secret

# Telegram Bot Setup
TELEGRAM_BOT_USERNAME=your_bot_username
TELEGRAM_BOT_TOKEN=your_bot_token

# Email Setup
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_app_password
```

### 2. Run the Application

**Using Gradle Wrapper:**

```bash
./gradlew bootRun
```

**Using Docker Compose:**

If you prefer a fully containerized environment (including PostgreSQL):
```bash
docker-compose up -d
```


