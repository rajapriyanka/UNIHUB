import React from "react";
import { NavLink } from "react-router-dom";
import "./FacultyNavbar.css";

const FacultyNavbar = () => {
  return (
    <nav className="fac-navbar">
      <div className="fac-logo">UNIHUB</div>
      <ul className="fac-nav-links">
      <li>
          <NavLink to="/faculty-dashboard" activeClassName="active">
            Dashboard
          </NavLink>
        </li>
        <li>
          <NavLink to="/fac-course-data" activeClassName="active">
            Courses
          </NavLink>
        </li>
        <li>
          <NavLink to="/fac-time-data" activeClassName="active">
            Time Table
          </NavLink>
        </li>
        <li>
          <NavLink to="/fac-leave-data" activeClassName="active">
            Leave
          </NavLink>
        </li>
        <li>
          <NavLink to="/fac-student-leave" activeClassName="active">
            Student Leave
          </NavLink>
        </li>
        <li>
          <NavLink to="/fac-attendance-data" activeClassName="active">
            Attendance
          </NavLink>
        </li>
        <li>
          <NavLink to="/fac-substitute-data" activeClassName="active">
           Substitute
          </NavLink>
        </li>
      </ul>
      <div className="fac-nav-right">
        <li>
          <NavLink to="/" activeClassName="active">
            Logout
          </NavLink>
        </li>
        <li>
          <NavLink to="/fac-profile" activeClassName="active">
            Profile
          </NavLink>
        </li>
      </div>
    </nav>
  );
};

export default FacultyNavbar;
