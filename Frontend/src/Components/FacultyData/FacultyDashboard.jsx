import React, { useEffect, useState } from "react";
import { motion } from "framer-motion";
import FacultyNavbar from "../Land/FacultyNavbar";
import FacultyService from "../../Service/FacultyService";
import "./FacultyDashboard.css";

const FacultyDashboard = () => {
  const [counts, setCounts] = useState({
    courses: 0,
    batches: 0,
    assignedCourses: 0,
  });

  useEffect(() => {
    const fetchCounts = async () => {
      try {
        const facultyId = localStorage.getItem("facultyId");

        const [courses, batches, assigned] = await Promise.all([
          FacultyService.getAllCourses(),
          FacultyService.getAllBatches(),
          FacultyService.getAssignedCourses(facultyId),
        ]);

        setCounts({
          courses: courses.length,
          batches: batches.length,
          assignedCourses: assigned.length,
        });
      } catch (error) {
        console.error("Error fetching faculty dashboard data:", error);
      }
    };

    fetchCounts();
  }, []);

  const cardVariants = {
    hidden: { opacity: 0, y: 50 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.5 } },
  };

  return (
    <div className="faculty-dashboard">
      <FacultyNavbar />
    <div className="faculty-dash-page">
      
      <div className="faculty-dash-container">
        
        <h1>Welcome to Faculty Dashboard</h1>
        <div className="dashboard-grid">
          {[
            { title: "Total Courses", count: counts.courses },
            { title: "Batches", count: counts.batches },
            { title: "Assigned Courses", count: counts.assignedCourses },
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

export default FacultyDashboard;
