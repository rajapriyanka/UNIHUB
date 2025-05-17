import React from "react";
import "./Mainsection.css";


const MainSection = () => {
  return (
    <div className="main-section">
      <div className="main-container">
        <h2>UNIHUB</h2>
        
        
          <ul className="buttons">
            <li>
              <span className="fas-fa-lock"></span>
              <button type="submit" className="admin-login"><a href="/admin/login">Admin Login</a></button>
            </li>
            <li>
              <button type="submit" className="faculty-login"><a href="/faculty/login">Faculty Login</a></button>
            </li>
            <li>
              <button type="submit" className="student-login"><a href="/student/login">Student Login</a></button>
            </li>
            
          </ul>
          
          
        
      </div>
      
    </div>
  );
};

export default MainSection;
