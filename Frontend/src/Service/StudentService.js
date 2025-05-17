import axios from "axios"





class StudentService {
  static BASE_URL = "http://localhost:8080"

  static async login(email, password) {
    try {
      console.log("🟢 Attempting login with:", { email, password }) // Log request

      const response = await axios.post(
        `${this.BASE_URL}/api/student/login`,
        { email, password },
        { headers: { "Content-Type": "application/json" } },
      )

      console.log("🟢 Login Success! Response:", response.data) // Log response

      if (response?.data?.jwt) {
        localStorage.setItem("token", response.data.jwt)
        localStorage.setItem("role", response.data.userRole)
        localStorage.setItem("studentId", response.data.id || "")
        return response.data
      } else {
        throw new Error("Invalid response from server.")
      }
    } catch (error) {
      console.error("🔴 Login failed. Server Response:", error.response?.data || error.message)
      throw new Error(error.response?.data?.message || "Login failed. Please try again.")
    }
  }

  static logout() {
    localStorage.removeItem("token")
    localStorage.removeItem("role")
    localStorage.removeItem("studentId")
    window.location.href = "/"
  }

  static isStudentAuthenticated() {
    return Boolean(localStorage.getItem("token"))
  }

  static isStudent() {
    return localStorage.getItem("role")?.toUpperCase() === "STUDENT"
  }

  static async getStudentProfile(email) {
    return axios.get(`${this.BASE_URL}/api/student/profile?email=${email}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    })
  }

  static async updateStudentProfile(profileData) {
    return axios.put(`${this.BASE_URL}/api/student/profile/update`, profileData, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    })
  }

  static async requestPasswordChangeOtp(email) {
    return axios.post(`${this.BASE_URL}/api/student/password/request-otp?email=${email}`)
  }

  static async changePassword(otpData) {
    return axios.post(`${this.BASE_URL}/api/student/password/change`, otpData)
  }

  // New methods for leave management
  static async requestLeave(studentId, leaveData) {
    return axios.post(`${this.BASE_URL}/api/student-leave/request/${studentId}`, leaveData, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    })
  }

  static async getLeaveHistory(studentId) {
    return axios.get(`${this.BASE_URL}/api/student-leave/student/${studentId}`, {
      headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
    })
  }
}

export default StudentService

