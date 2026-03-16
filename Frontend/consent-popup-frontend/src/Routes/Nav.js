import { BrowserRouter, Routes, Route } from "react-router-dom";
import LoadPopUp from "../Components/LoadPopUp";

const Nav = () => {
  return (
    <Routes>
      <Route path="/" element={<LoadPopUp />} />
    </Routes>
  );
};
export default Nav;
