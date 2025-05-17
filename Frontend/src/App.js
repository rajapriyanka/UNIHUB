import "./App.css";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import Mainsection from "./Components/Land/Mainsection";
import AdminLogin from "./Components/Login/AdminLogin";
import AdminDashboard from "./Components/AdminData/AdminDashboard";
import FacultyData from "./Components/AdminData/FacultyData";
import StudentData from "./Components/AdminData/StudentData";
import CourseData from "./Components/AdminData/CourseData";
import TimetableData from "./Components/AdminData/TimetableData";
import BatchData from "./Components/AdminData/BatchData";
import FacultyLogin from "./Components/Login/FacultyLogin";
import FacultyDashboard from "./Components/FacultyData/FacultyDashboard";
import FacultyCourse from "./Components/FacultyData/FacultyCourse";
import FacultyLeave from "./Components/FacultyData/FacultyLeave";
import FacultyTime from "./Components/FacultyData/FacultyTime";
import FacultySubstitute from "./Components/FacultyData/FacultySubstitute"; // New component
import TimetableGeneration from "./Components/Timetable/TimetableGeneration";
import ViewTimetable from "./Components/Timetable/ViewTimetable";
import { FacultyProvider } from "./Context/FacultyContext";
import { StudentProvider } from "./Context/StudentContext";
import UserService from "./Service/UserService";
import FacultyService from "./Service/FacultyService";
import StudentService from "./Service/StudentService";
import StudentLogin from "./Components/Login/StudentLogin";
import StudentDashboard from "./Components/StudentData/StudentDashboard"
import FacultyProfile from "./Components/FacultyData/FacultyProfile";
import StudentTimetableData from "./Components/StudentData/StudentTimetableData";
import AttendancePage from "./Components/FacultyData/AttendancePage";
import FacultyStudentLeaves from "./Components/FacultyData/FacultyStudentLeaves";
import StudentLeaveData from "./Components/StudentData/StudentLeaveData";
import StudentAttendanceData from "./Components/StudentData/StudentAttendanceData";
import StudentProfile from "./Components/StudentData/StudentProfile";
import SubstituteEmailAction from "./Components/EmailAction/SubstituteEmailAction"; 
import AdminProfile from "./Components/AdminData/AdminProfile";// New component

// Protected route for Admin
const AdminProtectedRoute = ({ element }) => {
  const isAuthenticated = UserService.isAuthenticated();
  const isAdmin = UserService.isAdmin();
  return isAuthenticated && isAdmin ? element : <Navigate to="/admin/login" />;
};

// Protected route for Faculty
const FacultyProtectedRoute = ({ element }) => {
  const isFacultyAuthenticated = FacultyService.isFacultyAuthenticated();
  const isFaculty = FacultyService.isFaculty();
  return isFacultyAuthenticated && isFaculty ? element : <Navigate to="/faculty/login" />;
};

const StudentProtectedRoute = ({ element }) => {
  const isStudentAuthenticated = StudentService.isStudentAuthenticated();
  const isStudent = StudentService.isStudent();
  return isStudentAuthenticated && isStudent ? element : <Navigate to="/student/login" />;
};

const App = () => {
  return (
    <StudentProvider>
      <FacultyProvider>
        <Router>
          <Routes>
            {/* Public route */}
            <Route path="/" element={<Mainsection />} />

            {/* Admin routes */}
            <Route path="/admin/login" element={<AdminLogin />} />
            <Route path="/admin-dashboard" element={<AdminProtectedRoute element={<AdminDashboard />} />} />
            <Route path="/faculty-data" element={<AdminProtectedRoute element={<FacultyData />} />} />
            <Route path="/student-data" element={<AdminProtectedRoute element={<StudentData />} />} />
            <Route path="/course-data" element={<AdminProtectedRoute element={<CourseData />} />} />
            <Route path="/timetable-data" element={<AdminProtectedRoute element={<TimetableData />} />} />
            <Route path="/batch-data" element={<AdminProtectedRoute element={<BatchData />} />} />
            <Route path="/view-all-timetables" element={<AdminProtectedRoute element={<ViewTimetable />} />} />
            <Route path="/admin-profile" element={<AdminProtectedRoute element={<AdminProfile />} />} />

            {/* Faculty routes */}
            <Route path="/faculty/login" element={<FacultyLogin />} />
            <Route path="/faculty-dashboard" element={<FacultyProtectedRoute element={<FacultyDashboard />} />} />
            <Route path="/fac-course-data" element={<FacultyProtectedRoute element={<FacultyCourse />} />} />
            <Route path="/fac-leave-data" element={<FacultyProtectedRoute element={<FacultyLeave />} />} />
            <Route path="/fac-time-data" element={<FacultyProtectedRoute element={<FacultyTime />} />} />
            <Route path="/fac-substitute-data" element={<FacultyProtectedRoute element={<FacultySubstitute />} />} />
            <Route path="/fac-attendance-data" element={<FacultyProtectedRoute element={<AttendancePage/>} />} />
            <Route path="/generate-timetable" element={<FacultyProtectedRoute element={<TimetableGeneration />} />} />
            <Route path="/view-timetable" element={<FacultyProtectedRoute element={<ViewTimetable />} />} />
            <Route path="/fac-profile" element={<FacultyProtectedRoute element={<FacultyProfile />} />} />
            <Route path="/fac-student-leave" element={<FacultyProtectedRoute element={<FacultyStudentLeaves />} />} />
            
            {/* Student route */}
            <Route path="/student/login" element={<StudentLogin />} />
            <Route path="/student-dashboard" element={<StudentProtectedRoute element={<StudentDashboard />} />} />
            <Route path="/stud-time-data" element={<StudentProtectedRoute element={<StudentTimetableData />} />} />
            <Route path="/stud-leave-data" element={<StudentProtectedRoute element={<StudentLeaveData />} />} />
            <Route path="/stud-attendance-data" element={<StudentProtectedRoute element={<StudentAttendanceData />} />} />
            <Route path="/stud-profile" element={<StudentProtectedRoute element={<StudentProfile />} />} />
            
            {/* Email action routes */}
            <Route path="/email-action" element={<SubstituteEmailAction />} />
            
            {/* Fallback route */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Router>
      </FacultyProvider>
    </StudentProvider>
  );
};

export default App;