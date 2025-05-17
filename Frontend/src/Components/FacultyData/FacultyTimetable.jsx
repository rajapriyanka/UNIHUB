import React, { useEffect, useState } from "react";
import axios from "axios";

const TimeTable = () => {
  const [facultyTimeTable, setFacultyTimeTable] = useState(null);
  const [batchTimeTable, setBatchTimeTable] = useState(null);
  const [facultyId, setFacultyId] = useState("");
  const [batchId, setBatchId] = useState("");
  const [generationResponse, setGenerationResponse] = useState(null);
  const [userRole, setUserRole] = useState("FACULTY"); // Track user role

  useEffect(() => {
    if (facultyId) {
      fetchFacultyTimeTable(facultyId);
    }
    if (batchId) {
      fetchBatchTimeTable(batchId);
    }
  }, [facultyId, batchId]);

  const fetchFacultyTimeTable = async (id) => {
    try {
      const response = await axios.get(`/api/timetable/faculty/${id}`);
      setFacultyTimeTable(response.data);
    } catch (error) {
      console.error("Error fetching faculty timetable:", error);
    }
  };

  const fetchBatchTimeTable = async (id) => {
    try {
      const response = await axios.get(`/api/timetable/batch/${id}`);
      setBatchTimeTable(response.data);
    } catch (error) {
      console.error("Error fetching batch timetable:", error);
    }
  };

  const generateTimeTable = async () => {
    if (userRole !== "ADMIN" && userRole !== "FACULTY") {
      alert("Only admins and faculty can generate the timetable.");
      return;
    }
    try {
      const response = await axios.post("/api/timetable/generate", {});
      setGenerationResponse(response.data);
    } catch (error) {
      console.error("Error generating timetable:", error);
    }
  };

  return (
    <div className="p-4">
      <h2 className="text-xl font-bold mb-4">TimeTable Viewer & Generator</h2>
      <div className="mb-4">
        <label className="mr-2">Faculty ID:</label>
        <input
          type="text"
          value={facultyId}
          onChange={(e) => setFacultyId(e.target.value)}
          className="border p-1"
        />
      </div>
      <div className="mb-4">
        <label className="mr-2">Batch ID:</label>
        <input
          type="text"
          value={batchId}
          onChange={(e) => setBatchId(e.target.value)}
          className="border p-1"
        />
      </div>
      {(userRole === "ADMIN" || userRole === "FACULTY") && (
        <button
          onClick={generateTimeTable}
          className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
        >
          Generate TimeTable
        </button>
      )}
      {generationResponse && (
        <div className="mt-4 p-4 border rounded">
          <h3 className="font-bold">Generated TimeTable</h3>
          <pre>{JSON.stringify(generationResponse, null, 2)}</pre>
        </div>
      )}
      {facultyTimeTable && (
        <div className="mt-4 p-4 border rounded">
          <h3 className="font-bold">Faculty Timetable</h3>
          <pre>{JSON.stringify(facultyTimeTable, null, 2)}</pre>
        </div>
      )}
      {batchTimeTable && (
        <div className="mt-4 p-4 border rounded">
          <h3 className="font-bold">Batch Timetable</h3>
          <pre>{JSON.stringify(batchTimeTable, null, 2)}</pre>
        </div>
      )}
    </div>
  );
};

export default TimeTable;
