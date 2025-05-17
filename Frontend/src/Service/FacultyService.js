import axios from "axios"

class FacultyService {
  static BASE_URL = "http://localhost:8080"

  // Utility to get the token
  static getToken() {
    const token = localStorage.getItem("adminToken") || localStorage.getItem("token")
    if (!token) {
      throw new Error("No authentication token found")
    }
    return token
  }

  static async login(email, password) {
    try {
      const response = await axios.post(`${this.BASE_URL}/api/faculty/login`, { email, password })
      if (response?.data?.jwt) {
        localStorage.setItem("token", response.data.jwt)
        localStorage.setItem("role", response.data.role)
        localStorage.setItem("facultyId", response.data.id || "")
        return response.data
      } else {
        throw new Error("Invalid response from server.")
      }
    } catch (error) {
      throw new Error(error.response?.data?.message || "Login failed. Please try again.")
    }
  }

  static logout() {
    localStorage.removeItem("token")
    localStorage.removeItem("role")
    localStorage.removeItem("facultyId")
    window.location.href = "/"
  }

  static isFacultyAuthenticated() {
    return Boolean(localStorage.getItem("token"))
  }

  static isFaculty() {
    return localStorage.getItem("role")?.toUpperCase() === "FACULTY"
  }

  // Faculty Profile Methods
  static async getFacultyProfile() {
    return this.apiGet("/api/faculty/profile")
  }

  static async updateFacultyProfile(profileData) {
    return this.apiPut("/api/faculty/profile", profileData)
  }

  static async requestPasswordChangeOtp() {
    return this.apiPost("/api/faculty/profile/request-password-change")
  }

  static async changePassword(otpData) {
    return this.apiPost("/api/faculty/profile/change-password", otpData)
  }

  static async getAllFaculties() {
    return this.apiGet("/api/admin/faculties")
  }

  static async getAllCourses() {
    return this.apiGet("/api/faculties/dashboard/courses")
  }

  static async getAllBatches() {
    try {
      console.log("FacultyService: Fetching all batches")
      const response = await axios.get(`${this.BASE_URL}/api/batches`, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      console.log("FacultyService: Batches received:", response.data)
      return response.data
    } catch (error) {
      console.error("FacultyService: Error fetching batches:", error)
      throw new Error(error.response?.data?.message || "Failed to fetch batches")
    }
  }

  static async addCourseToBatch(facultyId, courseId, batchId) {
    try {
      const response = await axios.post(
        `${this.BASE_URL}/api/faculties/dashboard/faculty/${facultyId}/courses/${courseId}/batches/${batchId}`,
        {},
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        },
      )

      // Check if the response contains an error message about another faculty
      if (
        response.data &&
        typeof response.data === "string" &&
        (response.data.includes("already assigned to") || response.data.includes("is already assigned"))
      ) {
        throw new Error(response.data)
      }

      return response.data
    } catch (error) {
      // Rethrow the error to be handled by the component
      if (error.response && error.response.data) {
        throw new Error(
          typeof error.response.data === "string"
            ? error.response.data
            : error.response.data.message || "Failed to assign course to batch",
        )
      }
      throw error
    }
  }

  static async getAssignedCourses(facultyId) {
    return this.apiGet(`/api/faculties/dashboard/faculty/${facultyId}/assigned-courses`)
  }

  static async removeCourse(facultyId, courseId, batchId) {
    return this.apiDelete(`/api/faculties/dashboard/${facultyId}/courses/${courseId}/batch/${batchId}`)
  }

  static async handleLeaveActionFromEmail(token, comment) {
    try {
      const result = await axios.get(`${this.BASE_URL}/api/email-actions/leave/${token}/api`, {
        params: { comment },
      })
      return { success: true, message: result.data.message }
    } catch (error) {
      console.error("Error processing email action:", error)
      return {
        success: false,
        message: error.response?.data || "Failed to process the leave request. The link may be invalid or expired.",
      }
    }
  }

  static async validateEmailActionToken(token) {
    try {
      const result = await axios.get(`${this.BASE_URL}/api/email-actions/leave/${token}/api`)
      return { isValid: true }
    } catch (error) {
      console.error("Error validating token:", error)
      return {
        isValid: false,
        message: error.response?.data || "This link is invalid or has expired.",
      }
    }
  }

  // Generic API Methods
  static async apiGet(url) {
    try {
      const response = await axios.get(`${this.BASE_URL}${url}`, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Request failed. Please try again.")
    }
  }

  static async apiPost(url, data = {}) {
    try {
      const response = await axios.post(`${this.BASE_URL}${url}`, data, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Request failed. Please try again.")
    }
  }

  static async apiPut(url, data = {}) {
    try {
      const response = await axios.put(`${this.BASE_URL}${url}`, data, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Request failed. Please try again.")
    }
  }

  static async apiDelete(url) {
    try {
      const response = await axios.delete(`${this.BASE_URL}${url}`, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Request failed. Please try again.")
    }
  }
}

export default FacultyService

