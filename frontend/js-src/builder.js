'use strict';

const e = React.createElement;

class Content extends React.Component {
    constructor(props) {
        super(props);
        this.walk = this.walk.bind(this);
        this.shuffle = this.shuffle.bind(this);
        this.handleUp = this.handleUp.bind(this);
        this.handleDown = this.handleDown.bind(this);
        this.dispatch = this.dispatch.bind(this);
        this.handleAdd = this.handleAdd.bind(this);
        this.handleDel = this.handleDel.bind(this);
        this.handleButton = this.handleButton.bind(this);
        this.state = {
            schedule: props.schedule,
            history: [],
            mode: props.mode,
            visible:  {table:"block",
                       builder:"none",
                       json:"none"}
        };
    }

    handleButton(id) {
        let update = {table:"none",
                      builder:"none",
                      json:"none"};
        update[id] = "block";
        console.log("update for " + id + " " + " is: " + JSON.stringify(this.state,null,2));
        this.setState({visible:update});
    }

    render() {
        return (
            <div id="react">
              <div onClick={() => this.handleButton("table")}>table</div>
              <div onClick={() => this.handleButton("builder")}>builder</div>
              <div onClick={() => this.handleButton("json")}>json</div>
              <div style={{display: this.state.visible.table}}></div>
              <div style={{display: this.state.visible.builder}}>
                <Schedule name={this.state.schedule.name}
                          playlists={this.state.schedule.playlists}
                          handlers={{
                              handleUp: this.handleUp,
                              handleDown: this.handleDown,
                              handleAdd: this.handleAdd,
                              handleDel: this.handleDel
                          }} />
              </div>
              <div style={{display: this.state.visible.json}}>
                <form method="post" action="schedule-builder.html">
                  <textarea className={this.state.schedule.name}
                            name="schedule-body"
                            readOnly={true}
                            value={JSON.stringify(this.state.schedule,null,2)} />
                  <input type="hidden"
                         readOnly={true}
                         name="mode"
                         value={this.state.mode} />
                  <input type="hidden"
                         readOnly={true}
                         name="schedule-name"
                         value={this.state.schedule.name} />
                  <input type="submit" value="Submit" />
                </form>
              </div>
            </div>
        );
    }

    dispatch(obj) {
        return {
            'playlist' : obj,
            'merge' : obj.playlists,
            'multi' : [obj.playlist],
            'complex' : obj.playlists
        }[obj.type];
    }

    walk(index) {
        let playlist = this.state.schedule.playlists;
        let idx = index.slice();
        while(idx.length > 1) {
            console.log("idx: " + idx + " playlist: " +
                        JSON.stringify(playlist));
            playlist = Array.isArray(playlist) ?
                this.dispatch(playlist[idx[0]]) :
                this.dispatch(playlist);
            idx = idx.slice(1,);
        }
        return [idx[0],playlist];
    }

    shuffle(index,change_idx) {
        let v = this.walk(index);
        let idx = parseInt(v[0]);
        let first_idx = idx + change_idx;
        let temp = v[1][first_idx];
        v[1][first_idx] = v[1][idx];
        v[1][idx] = temp;
    }

    handleUp(index) {
        this.shuffle(index,-1);
        this.setState({schedule: this.state.schedule,
                       history: this.state.history.concat([schedule])});
    }

    handleDown(index) {
        this.shuffle(index,1);
        this.setState({schedule: this.state.schedule,
                       history: this.state.history.concat([this.state.schedule])});
    }

    handleAdd(index,object) {
        find = index.length > 0 ? this.walk(index) : [undefined,this.state.schedule.playlists];
        index = find[0];
        console.log('found: ' + JSON.stringify(find));
        if(index == undefined) {
            find[1].push(object);
        } else {
            find[1][index]['playlists'].push(object);
        }
        this.setState({
            schedule: this.state.schedule,
            history: this.state.history.concat(this.state.schedule)
        });
    }

    handleDel(index) {
        find = this.walk(index);
        index = find[0];
        console.log('del: ' + JSON.stringify(find));
        find[1].splice(index,1);
        this.setState({
            schedule: this.state.schedule,
            history: this.state.history.concat(this.state.schedule)
        });
    }
}

function length(props) {
    return {
        'playlist': () => props.length,
        'merge': () => props.playlists.map(length).reduce((a,b) => a + b,0),
        'multi': () => length(props.playlist)/props.step,
        'complex': () => props.playlists.map(length)
            .reduce((a,b) =>
                    Math.max(a,b),0) * props.playlists.length,
    }[props.type]();
}

function makeUpDown(props,leftright) {
    let result = []
    let currentIndex = props.index[props.index.length-1];
    let up = "^";
    let down = "v";
    if (leftright == true) {
        up = "<";
        down = ">";
    }
    if(currentIndex != 0) {
        result.push(
            <Up up={props.handlers.handleUp}
                symbol={up}
                index={props.index}
                />);
    } else {
        result.push(<div className="blank-up"></div>)
    }
    result.push(<Del del={props.handlers.handleDel}
                symbol="X"
                index={props.index}
                />);
    if(currentIndex != props.lastIndex) {
        result.push(
            <Down down={props.handlers.handleDown}
                  symbol={down}
                  index={props.index}
                  />);
    }
    return (<div className="upDown">{result}</div>);
}

function Length(props) {
    return (<span>Length: {length(props)}</span>)
}

function Up(props) {
    return (<div className="up"
            onClick={() => props.up(props.index)}>
            {props.symbol}
            </div>)
}

function Del(props) {
    return (<div className="del"
            onClick={() => props.del(props.index)}>
            {props.symbol}
            </div>)
}

function Down(props) {
    return (<div className="down"
            onClick={() => props.down(props.index)}>
            {props.symbol}
            </div>)
}

function dispatch(handlers,index,lastIndex,obj) {
    return {
        'playlist': (<Playlist
                     index={index}
                     lastIndex={lastIndex}
                     handlers={handlers}
                     type={obj.type}
                     name={obj.name}
                     length={obj.length} />),
        'merge': (<Merge type={obj.type}
                  index={index}
                  lastIndex={lastIndex}
                  handlers={handlers}
                  playlists={obj.playlists}/>),
        'multi': (<Multi playlist={obj.playlist}
                  index={index}
                  handlers={handlers}
                  lastIndex={lastIndex}
                  type={obj.type}
                  step={obj.step}/>),
        'complex': (<Complex type={obj.type}
                    index={index}
                    lastIndex={lastIndex}
                    handlers={handlers}
                    playlists={obj.playlists}
                    />)
    }[obj.type];
}

function schedule_dispatch(handlers,index,lastIndex,obj) {
    return {
        'playlist': (<PlaylistHead type={obj.type}
                     index={index}
                     lastIndex={lastIndex}
                     handlers={handlers}
                     name={obj.name}
                     length={obj.length} />),
        'merge': (<Merge type={obj.type}
                  index={index}
                  lastIndex={lastIndex}
                  handlers={handlers}
                  playlists={obj.playlists}/>),
        'multi': (<Multi playlist={obj.playlist}
                  index={index}
                  lastIndex={lastIndex}
                  handlers={handlers}
                  type={obj.type}
                  step={obj.step}/>),
        'complex': (<Complex type={obj.type}
                    index={index}
                    lastIndex={lastIndex}
                    handlers={handlers}
                    playlists={obj.playlists}
                    />)
    }[obj.type];
}


function Playlist(props) {
    const up_down = makeUpDown(props);
    return (
        <li>
          {up_down}
          {props.name}: {props.length}
        </li>)
}

function PlaylistHead(props) {
    const up_down = makeUpDown(props,true);
    return (
        <div className="playlist block">
          {up_down}
          <div className="header">
            <strong>Playlist {props.name}
              <br/>
              <Length type={props.type}
                      length={props.length} />
            </strong>
          </div>
          <ol/>
        </div>
    );
}

function Merge(props) {
    const result = make_indexed_elements(props,
                                         props.index,
                                         props.playlists.length-1);
    const up_down = makeUpDown(props,true);

    return (
        <div className="merge block">
          {up_down}
          <div className="header">
            <strong>Merge
              <br/>
              <Length type={props.type}
                      playlists={props.playlists}/></strong>
          </div>
          <MakeElement handlers={props.handlers}
                       index={props.index} />
          <ol>{result}</ol>
        </div>
    )
}

function Multi(props) {
    const result = make_indexed_elements(props,props.index,0);
    const up_down = makeUpDown(props,true);

    return (
        <div className="multi block">
          {up_down}
          <div className="header">
            <strong>Multi
              <br/>
              <Length type={props.type}
                      step={props.step}
                      playlist={props.playlist}/></strong>
          </div>
          <ol>{result}</ol>
        </div>
    )
}

function Complex(props) {
    const result = make_indexed_elements(props,props.index,props.playlists.length-1);
    const index = props.index;
    const up_down = makeUpDown(props,true);

    return (
        <div className="complex block">
          {up_down}
          <div className="header">
            <strong>Complex
              <br/>
              <Length type={props.type}
                      playlists={props.playlists}/></strong>
          </div>
          <MakeElement handlers={props.handlers}
                       index={props.index} />
          <ol>{result}</ol>
        </div>
    )
}

function make_indexed_elements(props,index,lastIndex) {
    const result = [];
    if(undefined != props.playlist) {
        let t = index.slice();
        t.push(0);
        result.push(dispatch(props.handlers,t,0,props.playlist));
        return result;
    }
    for (let i in props.playlists) {
        let t = index.slice();
        t.push(i);
        result.push(dispatch(props.handlers,
                             t, lastIndex,
                             props.playlists[i]));
    }
    return result;
}

function Schedule(props) {
    const result = [];
    const lastIndex = props.playlists.length-1;
    for (let i in props.playlists) {
        result.push(schedule_dispatch(props.handlers
                                      ,[i]
                                      ,lastIndex,
                                      props.playlists[i]));
    }

    return (
        <div id="schedule">
          <h2>{props.name}</h2>
          <MakeElement handlers={props.handlers}
                       index={[]} />
          <ol className="schedule">
            {result}
          </ol>
        </div>
    )
}

class MakeElement extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            selectValue: 'playlist',
            selectPlaylist: all_playlists[0].props.value
        };
        console.log('playlist is? ' + JSON.stringify(this.state.selectPlaylist));
        this.handleChange = this.handleChange.bind(this);
        this.handleSubmit = this.handleSubmit.bind(this);
        this.handlePlaylistChange = this.handlePlaylistChange.bind(this);
        this.dispatch = this.dispatch.bind(this);
        console.log("GRRRR" + Object.keys(props.handlers));
        this.handleAdd = props.handlers.handleAdd;
        console.log("trying to make index at "+props.index);
        this.index = props.index;
    }

    handleChange(event) {
        this.setState({selectValue: event.target.value});
    }

    handlePlaylistChange(event) {
        this.setState({selectPlaylist: event.target.value});
    }

    dispatch() {
        let pattern = /(.*) (\d+)/;
        let result = pattern.exec(this.state.selectPlaylist);

        return ({
            'playlist': {'type':'playlist',
                         'name':result[1],
                         'length':parseInt(result[2])},
            'merge': {'type':'merge',
                      'playlists':[]},
            'multi': {'type':'multi',
                      'playlist':{"type":"merge","playlists":[]},
                      'step':0,
                      'start':0},
            'complex': {'type':'complex',
                        'playlists':[]}
        }[this.state.selectValue]);
    }

    handleSubmit(event) {
        console.log("this index " + this.index);
        this.handleAdd(this.index,this.dispatch());
        event.preventDefault();
    }

    render() {
        return (
            <div className="make">
              <select onChange={this.handlePlaylistChange}>{all_playlists}</select>
              <select
                value={this.state.selectValue}
                onChange={this.handleChange}>
                <option value="complex">complex</option>
                <option value="merge">merge</option>
                <option value="multi">multi</option>
                <option value="playlist">playlist</option>
              </select>
              <input name="add"
                     value="Add"
                     type="submit"
                     onClick={this.handleSubmit}
                     />
            </div>
        );
    }
}

var all_playlists = [];

document.addEventListener("DOMContentLoaded", () => {
    let t = document.querySelector('#playlist').querySelectorAll('option');
    for (let i in t) {
        all_playlists.push(<option value={t[i].text}>{t[i].text}</option>);
    }
    const mode = document.querySelector("input[name='mode']").value;
    console.log("mode is: " + mode);
    const domContainer = document.querySelector('#react');
    const schedule = document.querySelector("textarea[name='schedule-body']");
    const schedJson = schedule ? JSON.parse(schedule.textContent)
          : {name:'',playlists:[]};
    ReactDOM.render(e(Content,{schedule:schedJson,mode:mode}), domContainer);
});
