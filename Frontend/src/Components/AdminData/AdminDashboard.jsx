import React, { useEffect, useState } from "react";
import { motion } from "framer-motion";
import AdminNavbar from "../Land/AdminNavbar";
import UserService from "../../Service/UserService";
import "./AdminDashboard.css";

const AdminDashboard = () => {
  const [counts, setCounts] = useState({
    faculty: 0,
    students: 0,
    courses: 0,
    batches: 0,
  });

  useEffect(() => {
    const fetchCounts = async () => {
      try {
        const [faculty, students, courses, batches] = await Promise.all([
          UserService.getAllFaculty(),
          UserService.getAllStudents(),
          UserService.getAllCourses(),
          UserService.getAllBatches(),
        ]);

        setCounts({
          faculty: faculty.length,
          students: students.length,
          courses: courses.length,
          batches: batches.length,
        });
      } catch (error) {
        console.error("Error fetching data:", error);
      }
    };
    fetchCounts();
  }, []);

  const cardVariants = {
    hidden: { opacity: 0, y: 50 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.5 } },
  };

  return (
    <div className="admin-dashbaord">
      <AdminNavbar />
    <div className="admin-dash-page">
      
      <div className="admin-dash-container">
        <h1>Welcome to the Admin Dashboard!</h1>
        
        <div className="dashboard-grid">
          {[
            { title: "Faculty", count: counts.faculty },
            { title: "Students", count: counts.students },
            { title: "Batches", count: counts.batches },
            { title: "Courses", count: counts.courses },
            
          ].map((item, index) => (
            <motion.div
              key={index}
              className="dashboard-card"
              variants={cardVariants}
              initial="hidden"
              animate="visible"
            >
              <h2>{item.title}</h2>
              <p>{item.count}</p>
            </motion.div>
          ))}
        </div>
        
      </div>
    </div>
    </div>
  );
};

export default AdminDashboard;
