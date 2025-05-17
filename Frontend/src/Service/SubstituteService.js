import axios from "axios";

class SubstituteService {
  static BASE_URL = "http://localhost:8080";

  static getToken() {
    const token = localStorage.getItem("token");
    if (!token) {
      throw new Error("No token found. Please log in.");
    }
    return token;
  }

  static async filterFaculty(filterData) {
    try {
      const response = await axios.post(
        `${this.BASE_URL}/api/substitute/filter-faculty`,
        filterData,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        }
      );
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || "Failed to filter faculty"
      );
    }
  }

  static async createSubstituteRequest(requestData) {
    try {
      const response = await axios.post(
        `${this.BASE_URL}/api/substitute/request`,
        requestData,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        }
      );
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || "Failed to create substitute request"
      );
    }
  }

  static async updateRequestStatus(requestId, status, responseMessage) {
    try {
      const response = await axios.put(
        `${this.BASE_URL}/api/substitute/request/${requestId}/status`,
        null,
        {
          params: { status, responseMessage },
          headers: { Authorization: `Bearer ${this.getToken()}` },
        }
      );
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || "Failed to update request status"
      );
    }
  }

  static async getRequestsByRequester(facultyId) {
    try {
      const response = await axios.get(
        `${this.BASE_URL}/api/substitute/requests/requester/${facultyId}`,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        }
      );
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || "Failed to fetch requests"
      );
    }
  }

  static async getRequestsBySubstitute(facultyId) {
    try {
      const response = await axios.get(
        `${this.BASE_URL}/api/substitute/requests/substitute/${facultyId}`,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        }
      );
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || "Failed to fetch requests"
      );
    }
  }

  static async getPendingRequestsBySubstitute(facultyId) {
    try {
      const response = await axios.get(
        `${this.BASE_URL}/api/substitute/requests/substitute/${facultyId}/pending`,
        {
          headers: { Authorization: `Bearer ${this.getToken()}` },
        }
      );
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message || "Failed to fetch pending requests"
      );
    }
  }

  static async processRequestByToken(token, approved) {
    try {
      const response = await axios.get(
        `${this.BASE_URL}/api/substitute/process-token`,
        {
          params: { token, approved },
        }
      );
      return response.data;
    } catch (error) {
      throw new Error(
        error.response?.data?.message ||
          "Failed to process the substitute request. The link may be invalid or expired."
      );
    }
  }
}

export default SubstituteService;