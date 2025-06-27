import React, { createContext, useContext, useState, useEffect } from 'react';


const AuthContext = createContext();


export const AuthProvider = ({ children }) => {
    const [isLoggedIn, setIsLoggedIn] = useState(() => {
        return sessionStorage.getItem('isLoggedIn') === 'true';
    });
    
    const [userData, setUserData] = useState(() => {
        const storedUserData = sessionStorage.getItem('userData');
        return storedUserData ? JSON.parse(storedUserData) : null; 
    });

    useEffect(() => {
        sessionStorage.setItem('isLoggedIn', isLoggedIn); // Save login state
        sessionStorage.setItem('userData', JSON.stringify(userData)); // Save user data
    }, [isLoggedIn, userData]);

    return (
        <AuthContext.Provider value={{ isLoggedIn, setIsLoggedIn, userData, setUserData }}>
            {children}
        </AuthContext.Provider>
    );
};


export const useAuth = () => {
    return useContext(AuthContext);
};