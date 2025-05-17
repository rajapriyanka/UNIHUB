import React from "react";
import { NavLink } from "react-router-dom";
import "./AdminNavbar.css";

const Navbar = () => {
  return (
    <nav className="admin-navbar">
      <div className="admin-logo">UNIHUB</div>
      <ul className="admin-nav-links">
      <li>
          <NavLink to="/admin-dashboard" activeClassName="active">
            Dashboard
          </NavLink>
        </li>
        <li>
          <NavLink to="/faculty-data" activeClassName="active">
            Faculty
          </NavLink>
        </li>
        <li>
          <NavLink to="/student-data" activeClassName="active">
            Student
          </NavLink>
        </li>
        <li>
          <NavLink to="/batch-data" activeClassName="active">
            Batch
          </NavLink>
        </li>
        <li>
          <NavLink to="/course-data" activeClassName="active">
            Course
          </NavLink>
        </li>
        <li>
          <NavLink to="/timetable-data" activeClassName="active">
            Timetable
          </NavLink>
        </li>
      </ul>
      <div className="admin-nav-right">
        <li>
          <NavLink to="/" activeClassName="active">
            Logout
          </NavLink>
        </li>
        <li>
          <NavLink to="/admin-profile" activeClassName="active">
            Profile
          </NavLink>
        </li>
      </div>
    </nav>
  );
};

export default Navbar;
