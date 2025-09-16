export function authSanityCheck(login, stage, statusExpected) {
  client.test("auth sanity check", () => {
  const authStatus = JSON.parse(response.body)['stage'];
  client.log("Auth status for "+"login "+login+" is "+authStatus);
  client.assert(authStatus === statusExpected, 'Stage ' + stage + 'auth failed.');
})
}

// Names List is a list of contest names, ID-tag) pairs. We use the jsonData to store the
// contestID of the requested contest name into a global variable, indexed by the requested ID-tag.
// Returns a map from contest name to a list of (contestID, countyID) pairs.
export function getContestIDsWithCounties(jsonString) {

  const contests = new Map();
  const jsonData = JSON.parse(jsonString);

  console.log("JsonData = "+jsonData);
  // Initialize a map, with a key for each ID-tag and an empty list as its value.
  for(var i=0; i < jsonData.length ; i++) {

    const contestName = (jsonData[i]['name']);
    console.log("jsonData[i] = "+jsonData[i]);
    console.log("Found contest "+contestName);
    if(!contests.has(contestName)) {
      // We haven't seen this contest name before - add it to the map with an empty list of
      // contestID-counyID pairs.
      contests.set(contestName, []);
    }
    contests.get(contestName).push([jsonData[i]['id'],jsonData[i]['county_id']]);
    console.log("Adding contest"+contestName+", ID = "+jsonData[i]['id']+", countyID = "+jsonData[i]['county_id']);
  }

  return contests;
}