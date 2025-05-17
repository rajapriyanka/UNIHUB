import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import FacultyService from "../services/FacultyService";
import "./EmailLeaveAction.css";

const EmailLeaveAction = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const action = searchParams.get("action"); // "approve" or "reject"
  const [message, setMessage] = useState("Processing your request...");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token && (action === "approve" || action === "reject")) {
      const processLeaveAction = async () => {
        try {
          const response = await FacultyService.handleLeaveActionFromEmail(token);
          if (response.success) {
            setMessage(`Leave request has been successfully ${action}ed.`);
          } else {
            setMessage(response.message);
          }
        } catch (error) {
          setMessage("Error processing leave request. The link may be invalid or expired.");
        } finally {
          setLoading(false);
        }
      };

      processLeaveAction();
    } else {
      setMessage("Invalid leave action request.");
      setLoading(false);
    }
  }, [token, action]);

  return (
    <div className="email-leave-action">
      <h2>Email Leave Action</h2>
      {loading ? <p>Loading...</p> : <p>{message}</p>}
    </div>
  );
};

export default EmailLeaveAction;
