"use client"

import { createContext, useContext, useState, useEffect } from "react"

const StudentContext = createContext()

export const useStudent = () => useContext(StudentContext)

export const StudentProvider = ({ children }) => {
  const [studentInfo, setStudentInfo] = useState(() => {
    return {
      studentId: localStorage.getItem("studentId") || null, // Load from localStorage
      fullName: "",
      email: "",
      department: "",
    }
  })

  // Update localStorage when studentInfo changes
  useEffect(() => {
    if (studentInfo.studentId) {
      localStorage.setItem("studentId", studentInfo.studentId)
    }
  }, [studentInfo.studentId])

  const value = {
    studentInfo,
    setStudentInfo,
  }

  return <StudentContext.Provider value={value}>{children}</StudentContext.Provider>
}
