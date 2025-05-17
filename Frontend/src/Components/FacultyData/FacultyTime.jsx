"use client"

import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import FacultyNavbar from "../Land/FacultyNavbar"
import { useFaculty } from "../../Context/FacultyContext"
import "./FacultyTime.css"

const FacultyTime = () => {
  const { facultyData } = useFaculty()
  const [facultyName, setFacultyName] = useState("")
  const navigate = useNavigate()

  useEffect(() => {
    // Set faculty name from context or localStorage fallback
    if (facultyData && facultyData.fullName) {
      setFacultyName(facultyData.fullName)
    } else {
      const storedName = localStorage.getItem("facultyName")
      setFacultyName(storedName || "Faculty Member")
    }
  }, [facultyData])

  const handleNavigation = (path) => {
    navigate(path)
  }

  return (
    <div className="faculty-timetable-page">
      <FacultyNavbar />
      <div className="faculty-timetable-container">
        <div className="faculty-timetable-sidebar">
          <h2>Faculty Schedule</h2>
          <div className="card">
            
            <button className="generate-button" onClick={() => handleNavigation("/generate-timetable")}>
              Generate Timetable
            </button>
          </div>
          <div className="card">
            
            <button className="view-button" onClick={() => handleNavigation("/view-timetable")}>
              View Timetable
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default FacultyTime;

