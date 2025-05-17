"use client"

import { createContext, useContext, useState, useEffect } from "react"

// Create Faculty Context
const FacultyContext = createContext()

export const FacultyProvider = ({ children }) => {
  const [facultyData, setFacultyData] = useState(null)

  useEffect(() => {
    // Load faculty data from localStorage
    const storedFaculty = localStorage.getItem("facultyData")
    if (storedFaculty) {
      setFacultyData(JSON.parse(storedFaculty)) // Parse and set stored faculty data
    }
  }, [])

  // Set faculty information after login
  const setFacultyInfo = (facultyInfo) => {
    if (facultyInfo) {
      setFacultyData(facultyInfo)
      localStorage.setItem("facultyData", JSON.stringify(facultyInfo)) // Store full data
    }
  }

  // Clear faculty information during logout
  const clearFacultyInfo = () => {
    setFacultyData(null)
    localStorage.removeItem("facultyData")
  }

  return (
    <FacultyContext.Provider value={{ facultyData, setFacultyInfo, clearFacultyInfo }}>
      {children}
    </FacultyContext.Provider>
  )
}

// Custom hook to use the FacultyContext
export const useFaculty = () => useContext(FacultyContext)

