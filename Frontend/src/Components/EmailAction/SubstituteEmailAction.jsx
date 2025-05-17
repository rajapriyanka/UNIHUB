import React, { useState, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import SubstituteService from "../../Service/SubstituteService";
import "./EmailAction.css";

const SubstituteEmailAction = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [requestDetails, setRequestDetails] = useState(null);

  useEffect(() => {
    const token = searchParams.get("token");
    const action = searchParams.get("action");

    if (!token || !action) {
      setError("Invalid URL parameters");
      setLoading(false);
      return;
    }

    const isApprove = action === "approve-substitute";
    
    processToken(token, isApprove);
  }, [searchParams]);

  const processToken = async (token, isApprove) => {
    try {
      const result = await SubstituteService.processRequestByToken(token, isApprove);
      setRequestDetails(result);
      setSuccess(
        isApprove
          ? "You have successfully approved the substitute request."
          : "You have declined the substitute request."
      );
    } catch (error) {
      setError(error.message || "Failed to process the request. The link may be invalid or expired.");
    } finally {
      setLoading(false);
    }
  };

  const handleRedirect = () => {
    navigate("/faculty-dashboard");
  };

  if (loading) {
    return (
      <div className="email-action-container">
        <div className="email-action-card">
          <div className="loading-spinner"></div>
          <p>Processing your request...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="email-action-container">
      <div className="email-action-card">
        {error ? (
          <>
            <div className="action-icon error">
              <i className="fas fa-times-circle"></i>
            </div>
            <h2>Error</h2>
            <p>{error}</p>
          </>
        ) : (
          <>
            <div className="action-icon success">
              <i className="fas fa-check-circle"></i>
            </div>
            <h2>Success</h2>
            <p>{success}</p>
            {requestDetails && (
              <div className="request-details">
                <h3>Request Details</h3>
                <p><strong>Course:</strong> {requestDetails.courseTitle} ({requestDetails.courseCode})</p>
                <p><strong>Batch:</strong> {requestDetails.batchName} {requestDetails.section}</p>
                <p><strong>Date:</strong> {new Date(requestDetails.requestDate).toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</p>
                <p><strong>Time:</strong> Period {requestDetails.periodNumber} ({requestDetails.startTime} - {requestDetails.endTime})</p>
              </div>
            )}
          </>
        )}
        <button className="action-button" onClick={handleRedirect}>
          Go to Dashboard
        </button>
      </div>
    </div>
  );
};

export default SubstituteEmailAction;