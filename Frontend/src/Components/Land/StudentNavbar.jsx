import React from "react";
import { NavLink } from "react-router-dom";
import "./StudentNavbar.css";

const StudentNavbar = () => {
  return (
    <nav className="student-navbar">
      <div className="student-logo">UNIHUB</div>
      <ul className="student-nav-links">
      <li>
          <NavLink to="/student-dashboard" activeClassName="active">
            Dashboard
          </NavLink>
        </li>
        <li>
          <NavLink to="/stud-attendance-data" activeClassName="active">
            Attendance
          </NavLink>
        </li>
        <li>
          <NavLink to="/stud-time-data" activeClassName="active">
            Time Table
          </NavLink>
        </li>
        <li>
          <NavLink to="/stud-leave-data" activeClassName="active">
            Leave
          </NavLink>
        </li>
      </ul>
      <div className="stud-nav-right">
        <li>
          <NavLink to="/" activeClassName="active">
            Logout
          </NavLink>
        </li>
        <li>
          <NavLink to="/stud-profile" activeClassName="active">
            Profile
          </NavLink>
        </li>
      </div>
    </nav>
  );
};

export default StudentNavbar;
