import axios from "axios";

class StudentLeaveService {
  static BASE_URL = "http://localhost:8080";

  // Utility to get the token
  static getToken() {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("No token found. Please log in.");
    }
    return token;
  }

  // Request a new leave
  static async requestLeave(studentId, leaveData) {
    try {
      const response = await axios.post(
        `${this.BASE_URL}/api/student-leave/request/${studentId}`,
        leaveData,
        {
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${this.getToken()}`,
          },
        }
      );
      return response.data;
    } catch (error) {
      console.error("Error requesting leave:", error);
      throw new Error(
        error.response?.data?.message || "Failed to submit leave request. Please try again."
      );
    }
  }

  // Get leave history for a student
  static async getLeaveHistory(studentId) {
    try {
      const response = await axios.get(
        `${this.BASE_URL}/api/student-leave/student/${studentId}`,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        }
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching leave history:", error);
      throw new Error(
        error.response?.data?.message || "Failed to fetch leave history. Please try again."
      );
    }
  }

  // Get all faculties (for selecting a faculty to send leave request to)
  static async getAllFaculties() {
    try {
      const response = await axios.get(`${this.BASE_URL}/api/faculty`, {
        headers: { Authorization: `Bearer ${this.getToken()}` },
      });
      return response.data;
    } catch (error) {
      console.error("Error fetching faculties:", error);
      throw new Error(
        error.response?.data?.message || "Failed to fetch faculties. Please try again."
      );
    }
  }
}

export default StudentLeaveService;
