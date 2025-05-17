import axios from "axios"

class TimetableService {
  static BASE_URL = "http://localhost:8080"

  static getToken() {
    const token = localStorage.getItem("adminToken") || localStorage.getItem("token")
    if (!token) {
      throw new Error("No authentication token found")
    }
    return token
  }

  static async generateTimetable(facultyId, academicYear, semester) {
    try {
      const response = await axios.post(
        `${this.BASE_URL}/api/timetable/generate`,
        {
          facultyId,
          academicYear,
          semester,
        },
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        },
      )
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Failed to generate timetable")
    }
  }

  static async getFacultyTimetable(facultyId) {
    try {
      const response = await axios.get(`${this.BASE_URL}/api/timetable/faculty/${facultyId}`, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Failed to fetch faculty timetable")
    }
  }

  static async getBatchTimetable(batchId, academicYear, semester) {
    try {
      const response = await axios.get(`${this.BASE_URL}/api/timetable/batch/${batchId}`, {
        params: { academicYear, semester },
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Failed to fetch batch timetable")
    }
  }

  static async checkConflicts(timetableData) {
    try {
      const response = await axios.post(`${this.BASE_URL}/api/timetable/check-conflicts`, timetableData, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Failed to check timetable conflicts")
    }
  }

  static async saveTimetable(timetableData) {
    try {
      const response = await axios.post(`${this.BASE_URL}/api/timetable/save`, timetableData, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Failed to save timetable")
    }
  }

  static async updateTimetable(timetableId, timetableData) {
    try {
      const response = await axios.put(`${this.BASE_URL}/api/timetable/${timetableId}`, timetableData, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Failed to update timetable")
    }
  }

  static async deleteTimetable(timetableId) {
    try {
      const response = await axios.delete(`${this.BASE_URL}/api/timetable/${timetableId}`, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      })
      return response.data
    } catch (error) {
      throw new Error(error.response?.data?.message || "Failed to delete timetable")
    }
  }
}

export default TimetableService;

