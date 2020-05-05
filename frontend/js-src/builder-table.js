'use strict';

function MakeHeaderRow(props) {
    return (
        <thead>
          <tr>
            <th class="first" scope="col">Type: Length
              <br>RR (Repitition Rate)</th>
            <th colspan={props.maxLength}>Playlists</th>
          </tr>
        </thead>);
}

function MakeHead(props) {
    let name = props.type.charAt(0).toUpperCase() + props.type.slice(1);
    return (
        <tr className={props.type}>
          <th className="first" scope="row">{name}: {props.length}
            <br>RR: {props.rr}</th>
        </tr>
    );
}
