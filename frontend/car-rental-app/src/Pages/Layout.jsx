import React from 'react';
import { Outlet } from 'react-router-dom';
import NavBar from '../Components/NavBar';

function Layout() {
    return (
        <>
            <NavBar />
            <main className="page-content">
                <Outlet />
            </main>
        </>
    );
}

export default Layout;