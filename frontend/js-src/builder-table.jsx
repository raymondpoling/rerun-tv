'use strict';

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false };
    }

    static getDerivedStateFromError(error) {
        // Update state so the next render will show the fallback UI.
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        // You can also log the error to an error reporting service
        console.log(error);
        console.log(errorInfo);
    }
    render() {
        if (this.state.hasError) {
            // You can render any custom fallback UI
            return <h1>Something went wrong.</h1>;
        }

        return this.props.children;
    }
}

function HeaderRow(props) {
    return (
        <thead>
          <tr>
            <th className="first" scope="col">Type: Length
              <br/>RR (Repitition Rate)</th>
            <th colSpan={props.maxLength}>Playlists</th>
          </tr>
        </thead>);
}

function Head(props) {
    let name = props.obj.type.charAt(0).toUpperCase() + props.obj.type.slice(1);
    return (
        <th className="first" scope="row">{name} {props.obj.name}: {length(props.obj)}
          <br/>RR: {(length(props.obj)/props.medianLength).toFixed(2)}<br/>
          {props.obj.type === 'multi' ? "Step: " + props.obj.step : ""}</th>
    );
}

function colSize(obj,divisor) {
    return Math.round(length(obj)/divisor);
}

function PlaylistTD(props) {
    return (<td colSpan={colSize(props.obj,props.divisor)}>{props.obj.name}:
            {length(props.obj)}</td>);
}

function MergeTD(props) {
    return (<td colSpan={colSize(props.obj,props.divisor)}></td>);
}

function ComplexTD(props) {
    const out = props.obj.playlists.map(
        (a) =>
            <MakeRow obj={a} medianLength={props.medianLength/props.obj.playlists.length} divisor={props.divisor} />);
    return (<td colSpan={colSize(props.obj,props.divisor)}>
            <table><tbody>{out}</tbody></table>
            </td>);
}

function MultiTD(props) {
    const out = tableDispatch(props.obj.playlist).map((a) => tds(a,props.medianLength,props.divisor * props.obj.step));
    return out;
}

function MakeBody(props) {
    const rows = props.playlist
          .map((a) => <MakeRow obj={a} medianLength={props.medianLength} divisor={props.divisor} />);
    return (
        <tbody>
          <ErrorBoundary>
            {rows}
          </ErrorBoundary>
        </tbody>
    );
}

function tds(props,medianLength,divisor) {
    return {
        playlist: <PlaylistTD obj={props} medianLength={medianLength} divisor={divisor} />,
        merge: <MergeTD obj={props} medianLength={medianLength}
        divisor={divisor} />,
        multi: <MultiTD obj={props} medianLength={medianLength} divisor={divisor} />,
        complex: <ComplexTD obj={props} medianLength={medianLength} divisor={divisor} />
    }[props.type];
}

function tableDispatch(props) {
    return {
        playlist: [props],
        merge: props.playlists,
        multi: [props],
        complex: [props]
    }[props.type];
}

function MakeRow(props) {
    const out = tableDispatch(props.obj).map((a) =>tds(a,props.medianLength,props.divisor));
    return (
        <ErrorBoundary>
          <tr className={props.obj.type}>
            <Head obj={props.obj} medianLength={props.medianLength} divisor={props.divisor} />
            {out}
          </tr>
        </ErrorBoundary>
    );
}

function MakeTable(props) {
    const lengths = props.playlist.map(length);
    const maxLength = lengths.reduce((a,b) => Math.max(a,b),0);
    const medianLength = lengths.sort()[Math.floor(lengths.length/2)];
    return (
        <ErrorBoundary>
          <div id="table-holder">
            <table className="schedule">
              <HeaderRow maxLength={maxLength} divisor={1} />
              <MakeBody playlist={props.playlist}
                        medianLength={medianLength}
                        divisor={1} />
            </table>
          </div>
        </ErrorBoundary>
    );

}
