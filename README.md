
# UNIHUB 🎓

**UNIHUB** is a university management platform designed to streamline academic processes including content management, student-faculty interaction, and admin tasks. Built using Spring Boot for the backend and a modern frontend stack, the project aims to provide an efficient and intuitive experience for all university stakeholders.

---

## 📁 Folder Structure

```
UNIHUB/
│
├── CMS - Snapshots/        # Screenshots of the application
├── Frontend/               # Frontend source code (e.g. React)
├── Spring boot/            # Backend source code (Spring Boot)
└── README.md               # This file
```

---

## 🚀 Features

- 🔐 **User Authentication** (Login system)
- 🧑‍🏫 **Role-based Access** (Admin, Faculty, Student)
- 📅 **Timetable and Leave Management** with Substitute Management
- 🛠️ **Course Management** by Admin
- 🧾 **Batch Management** by Admin
- 📩 **Faculty Leave Management** with email notification and substitute handling
- 📤 **Faculty Attendance Upload** (Daily or Period-wise)
- 👨‍🎓 **Student Features**: View Attendance, Apply Leave, and View Timetable


---

## 🛠️ Tech Stack

| Layer        | Technology             |
|--------------|-------------------------|
| **Frontend** | React.js (or your stack)|
| **Backend**  | Spring Boot (Java)      |
| **Database** | MySQL / PostgreSQL      |
| **Build Tools** | Maven / npm           |
| **Hosting**  | Local / Render / Netlify|

---

## 🖼️ Screenshots

Below are the key screens of the UNIHUB system (add raw GitHub URLs once available):

### 🔐 Admin Login
<img width="1920" height="1080" alt="Admin login" src="https://github.com/user-attachments/assets/feb6b38f-9d05-4db0-9f03-9c6fca85ecd0" />

### 👤 Admin Profile Management
<img width="1920" height="1080" alt="Admin - profile (username change)" src="https://github.com/user-attachments/assets/ab719174-23d6-42d7-8a61-61628414c1d0" />

### 🔑 Admin Reset Password
<img width="1920" height="1080" alt="Admin - profile (password change)" src="https://github.com/user-attachments/assets/3741a073-cc52-40c1-a072-ac404d7aefcc" />

### 🔁 Faculty Request Substitute
<img width="1920" height="1080" alt="Finding available faculty for substitute" src="https://github.com/user-attachments/assets/c7e1a99e-bd9b-4d0d-9f80-17a8f0f9d0fe" />

### 📬 Faculty Substitute Requests
<img width="1920" height="1080" alt="Faculty Substitute request history" src="https://github.com/user-attachments/assets/29787730-a2cf-40fd-9c64-78639b298d49" />

### 📤 Faculty – Upload Attendance
<img width="1920" height="1080" alt="Uploading attendance" src="https://github.com/user-attachments/assets/7f6bcf5e-7c46-4922-9357-a15528aa1261" />

### 👨‍🎓 Student – View Attendance
<img width="1920" height="1080" alt="Student attendance percentage" src="https://github.com/user-attachments/assets/165cb958-35e1-4a64-962c-cfed8a8b7eed" />

---

## ⚙️ Running the Project

### 1. Clone the Repository

```bash
git clone https://github.com/rajapriyanka/UNIHUB.git
cd UNIHUB
```

### 2. Run Backend (Spring Boot)

```bash
cd "Spring boot"
./mvnw spring-boot:run
```

> Default port: `http://localhost:8080`

### 3. Run Frontend (React)

```bash
cd ../Frontend
npm install
npm start
```

> Default port: `http://localhost:3000`

---

## 👩‍💻 Author

- **Rajapriyanka R**
- 💼 [LinkedIn](https://www.linkedin.com/in/rajapriyankar/)
- 📧 rajapriyanka1101@gmail.com

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
