import axios from "axios"

class UserService {
  static BASE_URL = "http://localhost:8080"

  static async login(email, password) {
    try {
      console.log("Attempting login...")
      const response = await axios.post(`${UserService.BASE_URL}/api/login`, { email, password })

      if (response?.data) {
        console.log("Login successful, storing token and role")
        localStorage.setItem("token", response.data.jwt)
        localStorage.setItem("role", response.data.userRole)
        return response.data
      } else {
        throw new Error("Invalid response from server")
      }
    } catch (err) {
      console.error("Login error:", err)
      throw new Error(err.response?.data?.message || "Login failed. Please try again.")
    }
  }

  static logout() {
    console.log("Logging out, removing token and role")
    localStorage.removeItem("token")
    localStorage.removeItem("role")
  }

  static isAuthenticated() {
    const isAuth = Boolean(localStorage.getItem("token"))
    console.log("Checking authentication:", isAuth)
    return isAuth
  }

  static isAdmin() {
    const userRole = localStorage.getItem("role")
    const isAdminRole = userRole?.toUpperCase() === "ADMIN"
    console.log("Checking admin role:", isAdminRole)
    return isAdminRole
  }

  // Student methods
  static async registerStudent(studentData) {
    try {
      console.log("Registering new student:", studentData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.post(`${UserService.BASE_URL}/api/students`, studentData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Student registration successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Student registration error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Student registration failed. Please try again.")
    }
  }

  static async getAllStudents() {
    try {
      console.log("UserService: Fetching all students...")
      const token = localStorage.getItem("token")
      if (!token) {
        console.error("UserService: No token found")
        throw new Error("No token found. Please log in again.")
      }

      console.log("UserService: Making API request to:", `${UserService.BASE_URL}/api/students`)
      const response = await axios.get(`${UserService.BASE_URL}/api/students`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("UserService: API response for getAllStudents:", response.data)

      if (Array.isArray(response.data)) {
        console.log("UserService: Received an array of students. Length:", response.data.length)
        return response.data
      } else {
        console.error("UserService: Unexpected response format:", response.data)
        return []
      }
    } catch (err) {
      console.error("UserService: Get all students error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to fetch student list. Please try again.")
    }
  }

  static async updateStudent(id, studentData) {
    try {
      console.log(`Updating student with id ${id}:`, studentData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.put(`${UserService.BASE_URL}/api/students/${id}`, studentData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Student update successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Update student error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to update student. Please try again.")
    }
  }

  static async deleteStudent(id) {
    try {
      console.log(`Deleting student with id ${id}`)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.delete(`${UserService.BASE_URL}/api/students/${id}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Student deletion successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Delete student error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to delete student. Please try again.")
    }
  }

  static async searchStudentsByName(name) {
    try {
      console.log(`Searching students by name: ${name}`)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.get(`${UserService.BASE_URL}/api/students/search`, {
        params: { name },
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Student search successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Search students error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to search students. Please try again.")
    }
  }

  // Faculty methods
  static async registerFaculty(facultyData) {
    try {
      console.log("Registering new faculty:", facultyData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.post(`${UserService.BASE_URL}/api/faculty`, facultyData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Faculty registration successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Faculty registration error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Faculty registration failed. Please try again.")
    }
  }
  static async updateFaculty(userId, facultyData) {
    try {
      console.log(`Updating faculty with userId ${userId}:`, facultyData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.put(`${UserService.BASE_URL}/api/faculty/user/${userId}`, facultyData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Faculty update successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Update faculty error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to update faculty. Please try again.")
    }
  }

  static async getAllFaculty() {
    try {
      console.log("Fetching all faculty...")
      const token = localStorage.getItem("token")
      if (!token) throw new Error("No token found. Please log in again.")

      const response = await axios.get(`${UserService.BASE_URL}/api/faculty`, {
        headers: { Authorization: `Bearer ${token}` },
      })

      console.log("All Faculty Data:", response.data) // Log the complete response
      return response.data
    } catch (err) {
      console.error("Error fetching faculty:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to fetch faculty list. Please try again.")
    }
  }

  static async uploadFacultyExcel(file) {
    try {
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const formData = new FormData()
      formData.append("file", file)

      const response = await axios.post(`${UserService.BASE_URL}/api/faculty/upload`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      })

      return response.data
    } catch (err) {
      console.error("Faculty Excel upload error:", err.response?.data || err.message)
      throw new Error(err.response?.data || "Failed to upload Excel file. Please try again.")
    }
  }

  static async deleteFaculty(id) {
    try {
      console.log(`Attempting to delete faculty with ID: ${id}`)

      // Fetch all faculty and check if ID exists
      const facultyList = await UserService.getAllFaculty()
      const facultyExists = facultyList.some((faculty) => faculty.id === id)

      if (!facultyExists) {
        console.warn(`Faculty with ID ${id} not found in the system.`)
        return "Faculty not found or already deleted."
      }

      console.log(`Deleting faculty with ID ${id}`)
      const token = localStorage.getItem("token")
      if (!token) throw new Error("No token found. Please log in again.")

      const response = await axios.delete(`${UserService.BASE_URL}/api/faculty/user/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      })

      console.log("Faculty deletion successful:", response.data)
      return response.data
    } catch (err) {
      if (err.response?.status === 404) {
        console.warn(`Faculty with ID ${id} not found or already deleted.`)
        return "Faculty not found or already deleted."
      }
      console.error("Delete faculty error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to delete faculty. Please try again.")
    }
  }

  static async searchFacultyByName(name) {
    try {
      console.log(`Searching faculty by name: ${name}`)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.get(`${UserService.BASE_URL}/api/faculty/search`, {
        params: { name },
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Faculty search successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Search faculty error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to search faculty. Please try again.")
    }
  }

  // Course methods
  static async getAllCourses() {
    try {
      console.log("UserService: Fetching all courses...")
      const token = localStorage.getItem("token")
      if (!token) {
        console.error("UserService: No token found")
        throw new Error("No token found. Please log in again.")
      }

      console.log("UserService: Making API request to:", `${UserService.BASE_URL}/api/courses`)
      const response = await axios.get(`${UserService.BASE_URL}/api/courses`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("UserService: API response for getAllCourses:", response.data)

      if (Array.isArray(response.data)) {
        console.log("UserService: Received an array of courses. Length:", response.data.length)
        return response.data
      } else {
        console.error("UserService: Unexpected response format:", response.data)
        return []
      }
    } catch (err) {
      console.error("UserService: Get all courses error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to fetch course list. Please try again.")
    }
  }

  static async registerCourse(courseData) {
    try {
      console.log("Registering new course:", courseData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.post(`${UserService.BASE_URL}/api/courses`, courseData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Course registration successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Course registration error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Course registration failed. Please try again.")
    }
  }

  static async updateCourse(id, courseData) {
    try {
      console.log(`Updating course with id ${id}:`, courseData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.put(`${UserService.BASE_URL}/api/courses/${id}`, courseData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Course update successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Update course error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to update course. Please try again.")
    }
  }

  static async deleteCourse(id) {
    try {
      console.log(`Deleting course with id ${id}`)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.delete(`${UserService.BASE_URL}/api/courses/${id}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Course deletion successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Delete course error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to delete course. Please try again.")
    }
  }

  static async searchCourses(query) {
    try {
      console.log(`Searching courses with query: ${query}`)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.get(`${UserService.BASE_URL}/api/courses/search`, {
        params: { query },
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Course search successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Search courses error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to search courses. Please try again.")
    }
  }

  // Batch methods
  static async getAllBatches() {
    try {
      console.log("UserService: Fetching all batches...")
      const token = localStorage.getItem("token")
      if (!token) {
        console.error("UserService: No token found")
        throw new Error("No token found. Please log in again.")
      }

      console.log("UserService: Making API request to:", `${UserService.BASE_URL}/api/batches`)
      const response = await axios.get(`${UserService.BASE_URL}/api/batches`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("UserService: API response for getAllBatches:", response.data)

      if (Array.isArray(response.data)) {
        console.log("UserService: Received an array of batches. Length:", response.data.length)
        return response.data
      } else {
        console.error("UserService: Unexpected response format:", response.data)
        return []
      }
    } catch (err) {
      console.error("UserService: Get all batches error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to fetch batch list. Please try again.")
    }
  }

  static async registerBatch(batchData) {
    try {
      console.log("Registering new batch:", batchData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.post(`${UserService.BASE_URL}/api/batches`, batchData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Batch registration successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Batch registration error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Batch registration failed. Please try again.")
    }
  }

  static async updateBatch(id, batchData) {
    try {
      console.log(`Updating batch with id ${id}:`, batchData)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.put(`${UserService.BASE_URL}/api/batches/${id}`, batchData, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Batch update successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Update batch error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to update batch. Please try again.")
    }
  }

  static async deleteBatch(id) {
    try {
      console.log(`Deleting batch with id ${id}`)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.delete(`${UserService.BASE_URL}/api/batches/${id}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Batch deletion successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Delete batch error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to delete batch. Please try again.")
    }
  }

  static async searchBatches(query) {
    try {
      console.log(`Searching batches with query: ${query}`)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.get(`${UserService.BASE_URL}/api/batches/search`, {
        params: { query },
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log("Batch search successful:", response.data)
      return response.data
    } catch (err) {
      console.error("Search batches error:", err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || "Failed to search batches. Please try again.")
    }
  }

  static async uploadStudentExcel(formData) {
    try {
      console.log("Uploading student Excel file...");
      const token = localStorage.getItem("token");
      if (!token) {
        throw new Error("No token found. Please log in again.");
      }
  
      const response = await axios.post(`${UserService.BASE_URL}/api/students/upload`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      });
  
      console.log("Student Excel upload successful:", response.data);
      return response.data;
    } catch (err) {
      console.error("Student Excel upload error:", err.response?.data || err.message);
      throw new Error(err.response?.data || "Failed to upload student Excel file. Please try again.");
    }
  }
  
  static async search(resource, filters) {
    try {
      console.log(`Searching ${resource} with filters:`, filters)
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      const response = await axios.get(`${UserService.BASE_URL}/api/${resource}/search-by-both`, {
        params: filters,
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })

      console.log(`Search for ${resource} successful:`, response.data)
      return response.data
    } catch (err) {
      console.error(`Search ${resource} error:`, err.response?.data?.message || err.message)
      throw new Error(err.response?.data?.message || `Failed to search ${resource}. Please try again.`)
    }
  }

  static async uploadFacultyExcel(formData) {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        throw new Error("No token found. Please log in again.");
      }
  
      const response = await axios.post(`${UserService.BASE_URL}/api/faculty/upload`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      });
  
      console.log("Faculty Excel upload successful:", response.data);
      return response.data;
    } catch (err) {
      console.error("Faculty Excel upload error:", err.response?.data?.message || err.message);
      throw new Error(err.response?.data?.message || "Failed to upload Excel file. Please try again.");
    }
  }
  
  static async uploadCourseExcel(formData) {
    try {
      const token = localStorage.getItem("token")
      if (!token) {
        throw new Error("No token found. Please log in again.")
      }

      // Make sure formData contains the file
      const fileEntry = formData.get("file")
      if (!fileEntry) {
        throw new Error("No file selected for upload")
      }

      console.log("Uploading file:", fileEntry.name)

      const response = await axios.post(`${UserService.BASE_URL}/api/courses/upload`, formData, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      })

      return response.data
    } catch (err) {
      console.error("Course Excel upload error:", err.response?.data || err.message)
      throw err
    }
  }
}

export default UserService

