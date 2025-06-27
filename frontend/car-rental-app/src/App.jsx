import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './Pages/Layout';
import Home from './Pages/Home';
import Cars from './Pages/Cars';
import Login from './Pages/Login';
import Register from './Pages/Registration';
import { AuthProvider } from './Utils/AuthContext.jsx'; 
import CarBooking from './Components/CarBooking.jsx';
import CarReservation from './Components/CarReservation.jsx';

function App() {
    return (
        <AuthProvider> 
            <Router>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/register" element={<Register />} />
                    <Route path="/booking" element={<CarBooking/>}/>
                    <Route path="/reservation" element={<CarReservation/>}/>
                    <Route path="/" element={<Layout />}>
                        <Route index element={<Home />} />
                        <Route path="cars" element={<Cars />} />
                    </Route>
                </Routes>
            </Router>
        </AuthProvider>
    );
}

export default App;