import SkipToMain from 'corla/component/SkipToMain';
import * as React from 'react';

import {
    Alignment,
    Button,
    Classes,
    Intent,
    Navbar,
    NavbarDivider,
    NavbarGroup,
    NavbarHeading,
    Popover,
    Position,
    ProgressBar
} from '@blueprintjs/core';

import { Link } from 'react-router-dom';

import * as config from 'corla/config';

import resetDatabase from 'corla/action/dos/resetDatabase';
import logout from 'corla/action/logout';
import getAppInfo from 'corla/action/appInfo'

/**
 * Whether or not to show the reset button.
 */
function showResetButton(path: string) {
    return path === '/sos' && config.debug;
}

const MenuButton = () =>
    <Button icon='menu' minimal />;

interface IVersionButton {
    versionInfo: string,
    checking: boolean
}

declare const VERSION: boolean | React.ReactChild | React.ReactFragment | React.ReactPortal | null | undefined;

class VersionButton extends React.Component<any, any> {

    public state: IVersionButton = {
        versionInfo: "Checking",
        checking: false
    };

    public render() {
        return (
            <Popover position={Position.BOTTOM_LEFT} canEscapeKeyClose={true}
                enforceFocus={false} >
                <Button icon='info-sign' minimal onClick={this.getVersionInfo} />
                <div key="text" style={{margin: "10px", minWidth: "250px"}}>
                    <h5>RLA Info</h5>
                    <br/>
                    <this.displayVersion/>
                    <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 15 }}>
                        <Button className={Classes.POPOVER_DISMISS} intent={Intent.PRIMARY}>
                            Done
                        </Button>
                    </div>
                </div>
            </Popover>
        )
    }
    private displayVersion = () => {
        if (this.state.checking) {
            return (<p><ProgressBar className='pt-intent-primary'/></p>);
        } else {
        return (
            <><p><b>Backend Version:<span style={{ display: "inline-block", float: "right" }}>{this.state.versionInfo}</span></b></p>
                <p><b>Front End Version:<span style={{ display: "inline-block", float: "right" }}>{VERSION}</span></b></p></>
        );
        }
    }
    private getVersionInfo = () => {
        this.state.checking = true ;
        var v = getAppInfo().then(r => this.setState({ versionInfo: r.versionInfo, checking: false }))
        .catch(e=>{this.setState({ versionInfo: e, checking: false })});
    }

}

const Heading = () =>
    <NavbarHeading>Colorado RLA</NavbarHeading>;

const Divider = () =>
    <NavbarDivider />;

interface HomeButtonProps {
    path: string;
}

const HomeButton = ({ path }: HomeButtonProps) => (
    <Link to={ path }>
        <Button icon='home' minimal text='Home' />
    </Link>
);

interface LogoutButtonProps {
    logout: OnClick;
}

const LogoutButton = ({ logout: logoutAction }: LogoutButtonProps) =>
    <Button icon='log-out' minimal onClick={logoutAction} text='Log out' />;

interface ResetButtonProps {
    reset: OnClick;
}

const ResetDatabaseButton = ({ reset }: ResetButtonProps) => (
    <Button icon='warning-sign'>
        DANGER: Reset Database
    </Button>
);




export default function withNav(Menu: React.ComponentClass, path: string) {
    return () => (
        <Navbar className='l-nav'>
            <NavbarGroup align={Alignment.LEFT}>
                <SkipToMain />
                <Popover content={<Menu />} position={Position.RIGHT_TOP}>
                    <MenuButton />
                </Popover>
                <Heading />
            </NavbarGroup>
            <NavbarGroup align={Alignment.RIGHT}>
                {showResetButton(path) && <ResetDatabaseButton reset={resetDatabase} />}
                {showResetButton(path) && <Divider />}
                <HomeButton path={path} />
                <Divider />
                <LogoutButton logout={logout} />
                <Divider />
                <VersionButton/>
            </NavbarGroup>
        </Navbar>
    );
}
