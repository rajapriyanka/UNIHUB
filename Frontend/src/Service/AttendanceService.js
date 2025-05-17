import axios from "axios"

class AttendanceService {
  static BASE_URL = "http://localhost:8080"

  // Utility to get the token
  static getToken() {
    const token = localStorage.getItem("token")
    if (!token) {
      throw new Error("No token found. Please log in.")
    }
    return token
  }

  // Generate attendance template for a specific course and batch
  static async generateAttendanceTemplate(facultyId, courseId, batchName, department = null, section = null) {
    try {
      let url = `${this.BASE_URL}/api/attendance/template?facultyId=${facultyId}&courseId=${courseId}&batchName=${batchName}`
      
      // Add department parameter if provided
      if (department) {
        url += `&department=${encodeURIComponent(department)}`
      }
      
      // Add section parameter if provided
      if (section) {
        url += `&section=${encodeURIComponent(section)}`
      }
      
      const response = await axios.get(
        url,
        {
          headers: { 
            Authorization: `Bearer ${this.getToken()}`,
          },
          responseType: 'blob', // Important for file download
        }
      )
      
      // Create a download link for the file
      const url2 = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url2
      
      // Include department and section in filename if provided
      let filename = `attendance_template_${courseId}_${batchName}`
      if (department) filename += `_${department}`
      if (section) filename += `_${section}`
      filename += '.xlsx'
      
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      
      return { success: true, message: "Template downloaded successfully" }
    } catch (error) {
      console.error("Error generating template:", error)
      throw new Error(error.response?.data?.message || "Failed to generate attendance template")
    }
  }

  // Upload filled attendance Excel file
  static async uploadAttendance(facultyId, file, department = null, section = null) {
    try {
      const formData = new FormData()
      formData.append('file', file)
      
      let url = `${this.BASE_URL}/api/attendance/upload?facultyId=${facultyId}`
      
      // Add department parameter if provided
      if (department) {
        url += `&department=${encodeURIComponent(department)}`
      }
      
      // Add section parameter if provided
      if (section) {
        url += `&section=${encodeURIComponent(section)}`
      }
      
      const response = await axios.post(
        url,
        formData,
        {
          headers: { 
            Authorization: `Bearer ${this.getToken()}`,
            'Content-Type': 'multipart/form-data'
          }
        }
      )
      
      return response.data
    } catch (error) {
      console.error("Error uploading attendance:", error)
      throw new Error(error.response?.data?.message || "Failed to upload attendance data")
    }
  }

  // Get attendance records for a faculty's course and batch
  static async getAttendanceByFacultyCourseAndBatch(facultyId, courseId, batchName, department = null, section = null) {
    try {
      let url = `${this.BASE_URL}/api/attendance/faculty/${facultyId}/course/${courseId}/batch/${batchName}`
      
      // Add query parameters if provided
      const params = new URLSearchParams()
      if (department) params.append('department', department)
      if (section) params.append('section', section)
      
      if (params.toString()) {
        url += `?${params.toString()}`
      }
      
      const response = await axios.get(
        url,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` }
        }
      )
      
      return response.data
    } catch (error) {
      console.error("Error fetching attendance:", error)
      throw new Error(error.response?.data?.message || "Failed to fetch attendance records")
    }
  }

  // Get attendance records for a student in a specific course
  static async getStudentAttendanceForCourse(studentId, courseId) {
    try {
      const response = await axios.get(
        `${this.BASE_URL}/api/attendance/student/${studentId}/course/${courseId}`,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` }
        }
      )
      
      return response.data
    } catch (error) {
      console.error("Error fetching student attendance:", error)
      throw new Error(error.response?.data?.message || "Failed to fetch student attendance records")
    }
  }

  // Get attendance percentage for a student across all courses
  static async getStudentAttendancePercentage(studentId, semesterNo = null) {
    try {
      let url = `${this.BASE_URL}/api/attendance/student/${studentId}/percentage`
      if (semesterNo) {
        url += `?semesterNo=${semesterNo}`
      }
      
      const response = await axios.get(url, {
        headers: { Authorization: `Bearer ${this.getToken()}` }
      })
      
      return response.data
    } catch (error) {
      console.error("Error fetching attendance percentage:", error)
      throw new Error(error.response?.data?.message || "Failed to fetch attendance percentage")
    }
  }

  // Generate attendance report Excel for a course and batch
  static async generateAttendanceReport(facultyId, courseId, batchName, department = null, section = null) {
    try {
      let url = `${this.BASE_URL}/api/attendance/report?facultyId=${facultyId}&courseId=${courseId}&batchName=${batchName}`
      
      // Add department parameter if provided
      if (department) {
        url += `&department=${encodeURIComponent(department)}`
      }
      
      // Add section parameter if provided
      if (section) {
        url += `&section=${encodeURIComponent(section)}`
      }
      
      const response = await axios.get(
        url,
        {
          headers: { 
            Authorization: `Bearer ${this.getToken()}`,
          },
          responseType: 'blob', // Important for file download
        }
      )
      
      // Create a download link for the file
      const url2 = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url2
      
      // Include department and section in filename if provided
      let filename = `attendance_report_${courseId}_${batchName}`
      if (department) filename += `_${department}`
      if (section) filename += `_${section}`
      filename += '.xlsx'
      
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      
      return { success: true, message: "Report downloaded successfully" }
    } catch (error) {
      console.error("Error generating report:", error)
      throw new Error(error.response?.data?.message || "Failed to generate attendance report")
    }
  }

  // Check attendance and send notifications
  static async checkAttendanceAndNotify() {
    try {
      const response = await axios.post(
        `${this.BASE_URL}/api/attendance/check-and-notify`,
        {},
        {
          headers: { Authorization: `Bearer ${this.getToken()}` }
        }
      )
      
      return response.data
    } catch (error) {
      console.error("Error checking attendance:", error)
      throw new Error(error.response?.data?.message || "Failed to check attendance and send notifications")
    }
  }
}

export default AttendanceService